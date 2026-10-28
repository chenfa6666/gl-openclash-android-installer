package com.chenfa.openclashinstaller.core.ext

import kotlin.math.ln
import kotlin.math.pow

/** 大小写不敏感包含。等价 C++ ContainsCI。 */
fun String.containsCi(needle: String): Boolean = lowercase().contains(needle.lowercase())

/**
 * 格式化速度（自动 B/s / KB/s / MB/s）。
 * 等价 Windows 版 1024 阈值换算。
 */
fun formatSpeed(bps: Double): String {
    if (bps < 1.0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    val digit = (ln(bps) / ln(1024.0)).toInt().coerceAtMost(units.size - 1)
    val v = bps / 1024.0.pow(digit.toDouble())
    return "%.2f %s".format(v, units[digit])
}

/** 格式化字节数（不带 /s 后缀）。 */
fun formatBytes(bytes: Long): String {
    if (bytes < 1L) return "0 B"
    val v = bytes.toDouble()
    val units = arrayOf("B", "KB", "MB", "GB")
    val digit = (ln(v) / ln(1024.0)).toInt().coerceAtMost(units.size - 1)
    val n = v / 1024.0.pow(digit.toDouble())
    return "%.2f %s".format(n, units[digit])
}

/** 人类可读的耗时：860ms / 8.4 秒 / 2 分 13 秒。 */
fun formatDuration(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    return when {
        safe < 1000L -> "${safe}ms"
        safe < 60_000L -> "%.1f 秒".format(safe / 1000.0)
        else -> {
            val totalSeconds = safe / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            if (seconds == 0L) "${minutes} 分" else "${minutes} 分 ${seconds} 秒"
        }
    }
}
