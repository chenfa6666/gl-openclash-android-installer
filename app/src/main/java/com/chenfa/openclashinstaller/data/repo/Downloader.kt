package com.chenfa.openclashinstaller.data.repo

import com.chenfa.openclashinstaller.data.model.DownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * OkHttp 下载器。
 *
 * 关键行为：
 *  - 先写 `*.part` 临时文件，成功后原子替换目标文件，失败/取消不会破坏已有安装包；
 *  - 节流 300ms 输出瞬时/平均速度，首包立即输出；
 *  - 协程取消时主动 `call.cancel()`，避免读 socket 阻塞导致「强制结束」迟迟不生效。
 */
class Downloader(
    private val client: OkHttpClient,
) {
    companion object {
        private const val THROTTLE_MS = 300L
        private const val BUF_SIZE = 64 * 1024
    }

    /**
     * 下载到 destPath。
     * @return Flow<DownloadProgress>，每帧节流 300ms；末帧 done=true
     */
    fun download(url: String, destPath: String): Flow<DownloadProgress> = flow {
        val destination = File(destPath)
        val parent = destination.parentFile ?: File(".")
        parent.mkdirs()
        val partial = File(parent, destination.name + ".part")

        val call = client.newCall(Request.Builder().url(url).build())
        val job = currentCoroutineContext()[Job]
        val cancelHandle = job?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }

        var completed = false
        var received = 0L
        var total = -1L
        var elapsedMs = 0L
        var avgBps = 0.0

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code} ${response.message}")
                }
                val body = response.body ?: throw RuntimeException("HTTP 响应为空")
                total = body.contentLength()
                val start = System.currentTimeMillis()
                var lastEmit = 0L
                var lastReceived = 0L

                // 首包立即输出
                emit(DownloadProgress(0L, total, 0.0, 0.0, 0L, done = false))

                FileOutputStream(partial).use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(BUF_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            received += n
                            val now = System.currentTimeMillis()
                            elapsedMs = (now - start).coerceAtLeast(1L)
                            if (now - lastEmit >= THROTTLE_MS) {
                                val dt = (now - lastEmit).coerceAtLeast(1) / 1000.0
                                val inst = (received - lastReceived) / dt
                                avgBps = received / (elapsedMs / 1000.0)
                                emit(
                                    DownloadProgress(
                                        received = received,
                                        total = total,
                                        instBps = inst,
                                        avgBps = avgBps,
                                        elapsedMs = elapsedMs,
                                        done = false,
                                    ),
                                )
                                lastEmit = now
                                lastReceived = received
                            }
                        }
                    }
                }
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                avgBps = received / (elapsedMs / 1000.0)
            }

            // 原子替换：先尝试 rename，失败再退化为 copy + delete
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                if (!partial.delete()) partial.deleteOnExit()
            }
            completed = true
            emit(
                DownloadProgress(
                    received = received,
                    total = total,
                    instBps = 0.0,
                    avgBps = avgBps,
                    elapsedMs = elapsedMs,
                    done = true,
                ),
            )
        } finally {
            cancelHandle?.dispose()
            if (!completed) {
                if (!currentCoroutineContext().isActive) call.cancel()
                partial.delete()
            }
        }
    }.flowOn(Dispatchers.IO)
}
