package com.chenfa.openclashinstaller.data.model

/**
 * 日志条目。
 *
 * @param id              单调递增，Compose LazyColumn 用作稳定 key
 * @param time            展示时间（HH:mm:ss）
 * @param text            文本（不含末尾换行）
 * @param kind            着色 / 排版种类
 * @param progressKey     非 null 时表示进度行；UI 用同 key 覆盖最后一条达到原地刷新
 * @param progressPercent 进度百分比（0..100）；未知为 null
 */
data class LogEntry(
    val id: Long,
    val time: String,
    val text: String,
    val kind: LogKind = LogKind.NORMAL,
    val progressKey: String? = null,
    val progressPercent: Int? = null,
)
