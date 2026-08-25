package com.chenfa.openclashinstaller.data.model

/**
 * UI 单一状态。
 * 阶段 B：仅 envStatus / fields / settingsOpen / aboutOpen 接线。
 * 阶段 C+：logEntries / busy / activeOp / currentProgress / downloadedFiles 后续接入。
 */
data class UiState(
    val envStatus: List<EnvItem> = emptyList(),
    val fields: ConnFields = ConnFields(),
    val downloadedFiles: DownloadedFiles = DownloadedFiles(),
    val logEntries: List<LogEntry> = emptyList(),
    val busy: Boolean = false,
    val activeOp: OpId? = null,
    val currentProgress: String? = null,
    val settingsOpen: Boolean = false,
    val aboutOpen: Boolean = false,
    val confirmAbortOpen: Boolean = false,
)
