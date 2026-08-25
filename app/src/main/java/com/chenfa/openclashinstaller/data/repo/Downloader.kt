package com.chenfa.openclashinstaller.data.repo

import com.chenfa.openclashinstaller.data.model.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * OkHttp 下载器。
 * 等价 Windows 版 WinHttpDownload：节流 300ms 输出瞬时/平均速度，首包立即输出，可取消。
 *
 * 取消机制：协程 cancel 时 byteStream().read 抛 IOException（OkHttp 转 CancellationException），
 * flow 自动结束；outputStream 在 finally 关闭，避免文件句柄泄漏。
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
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw RuntimeException("HTTP ${resp.code} ${resp.message}")
        }
        val total = resp.body?.contentLength() ?: -1L
        val body = resp.body ?: throw RuntimeException("HTTP body 为空")
        val start = System.currentTimeMillis()
        var received = 0L
        var lastEmit = 0L
        var lastReceived = 0L

        // 首包立即输出
        emit(DownloadProgress(received, total, 0.0, 0.0, done = false))

        FileOutputStream(destPath).use { out ->
            body.byteStream().use { input ->
                val buf = ByteArray(BUF_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    received += n
                    val now = System.currentTimeMillis()
                    if (now - lastEmit >= THROTTLE_MS) {
                        val dt = (now - lastEmit).coerceAtLeast(1) / 1000.0
                        val inst = (received - lastReceived) / dt
                        val avg = received / ((now - start).coerceAtLeast(1) / 1000.0)
                        emit(DownloadProgress(received, total, inst, avg, done = false))
                        lastEmit = now
                        lastReceived = received
                    }
                }
            }
        }

        // 末帧
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1) / 1000.0
        emit(DownloadProgress(received, total, 0.0, received / elapsed, done = true))
    }.flowOn(Dispatchers.IO)
}
