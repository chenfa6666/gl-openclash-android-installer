package com.chenfa.openclashinstaller.data.model

/**
 * 日志条目。
 *
 * @param id          单调递增，Compose LazyColumn 用作稳定 key
 * @param text        文本（不含末尾换行）
 * @param kind        着色种类
 * @param progressKey 非 null 时表示进度行；UI 用同 key 覆盖最后一条达到原地刷新
 */
data class LogEntry(
    val id: Long,
    val text: String,
    val kind: LogKind = LogKind.NORMAL,
    val progressKey: String? = null,
)
