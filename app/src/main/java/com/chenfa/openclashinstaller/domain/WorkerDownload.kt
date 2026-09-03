package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.core.ext.formatSpeed
import com.chenfa.openclashinstaller.data.repo.Downloader
import kotlinx.coroutines.flow.collect

/**
 * 下载用例：选 URL → 调 Downloader → 回写日志（含进度单行原地刷新）。
 * 等价 Windows 版 WorkerDownload + WinHttpDownload。
 *
 * @param onLog      普通日志回调（追加，suspend）
 * @param onProgress 进度日志回调（同 key 覆盖最后一条，suspend）
 */
class WorkerDownload(
    private val downloader: Downloader,
    private val onLog: suspend (String) -> Unit,
    private val onProgress: suspend (String) -> Unit,
) {
    /**
     * @return true 下载成功
     */
    suspend fun execute(url: String, destPath: String, label: String): Boolean {
        onLog(" 下载 $label ")
        onLog("URL: $url")
        return try {
            downloader.download(url, destPath).collect { p ->
                if (p.done) {
                    val total = if (p.total > 0) " / ${formatBytes(p.total)}" else ""
                    onProgress("· 最终: ${formatBytes(p.received)}$total   平均速度: ${formatSpeed(p.avgBps)}")
                    onLog("✓ 下载完成: ${formatBytes(p.received)}")
                } else {
                    val total = if (p.total > 0) " / ${formatBytes(p.total)}" else ""
                    val pct = if (p.percent >= 0) " (${p.percent}%)" else ""
                    onProgress("· 已下载: ${formatBytes(p.received)}$total$pct   速度: ${formatSpeed(p.instBps)}")
                }
            }
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            onProgress("! 下载已被强制取消")
            throw e
        } catch (e: Throwable) {
            onLog("✗ 下载失败: ${e.message}")
            false
        }
    }
}
