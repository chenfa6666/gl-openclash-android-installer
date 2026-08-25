package com.chenfa.openclashinstaller.data.model

/**
 * 下载进度数据。等价 Windows 版 WinHttpDownload 节流 300ms 输出的瞬时/平均速度。
 *
 * @param received 已下载字节
 * @param total    总字节（Content-Length；未知为 -1）
 * @param instBps  瞬时速度 B/s
 * @param avgBps   平均速度 B/s
 * @param done     是否完成
 */
data class DownloadProgress(
    val received: Long,
    val total: Long,
    val instBps: Double,
    val avgBps: Double,
    val done: Boolean,
) {
    val percent: Int
        get() = if (total > 0) ((received * 100) / total).toInt().coerceIn(0, 100) else -1
}
