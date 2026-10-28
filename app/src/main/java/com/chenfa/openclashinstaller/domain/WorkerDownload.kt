package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.core.ext.formatDuration
import com.chenfa.openclashinstaller.core.ext.formatSpeed
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.repo.Downloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect

/**
 * 下载用例：选 URL → 调 Downloader → 回写日志（含进度单行原地刷新）。
 *
 * 日志约定：进度行用 `·` 开头，完成行用 `✓`，失败行用 `✗`；
 * ViewModel 会按行首符号自动着色。
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
        onLog("开始下载 $label")
        onLog("· 地址：$url")
        return try {
            downloader.download(url, destPath).collect { p ->
                if (p.done) {
                    val total = if (p.total > 0) " / ${formatBytes(p.total)}" else ""
                    onProgress("· 100% · ${formatBytes(p.received)}$total · 平均 ${formatSpeed(p.avgBps)}")
                    onLog("✓ $label 下载完成 · 用时 ${formatDuration(p.elapsedMs)}")
                } else {
                    val total = if (p.total > 0) " / ${formatBytes(p.total)}" else ""
                    val pct = if (p.percent >= 0) " · ${p.percent}%" else ""
                    onProgress("· 已下载 ${formatBytes(p.received)}$total$pct · ${formatSpeed(p.instBps)}")
                }
            }
            true
        } catch (e: CancellationException) {
            onLog("! 下载已取消")
            throw e
        } catch (e: Throwable) {
            currentCoroutineContext().ensureActive()
            onLog("✗ $label 下载失败：${e.toUserMessage("请检查网络或下载地址后重试")}")
            false
        }
    }
}
