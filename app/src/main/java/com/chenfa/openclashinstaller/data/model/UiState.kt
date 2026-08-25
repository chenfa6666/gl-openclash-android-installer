package com.chenfa.openclashinstaller.data.model

import com.chenfa.openclashinstaller.core.Constants

/**
 * UI 单一状态。
 * 阶段 B：envStatus / fields / 3 个 URL / 对话框开关接线。
 * 阶段 C+：logEntries / busy / activeOp / currentProgress / downloadedFiles 后续接入。
 */
data class UiState(
    val envStatus: List<EnvItem> = emptyList(),
    val fields: ConnFields = ConnFields(),
    val downloadedFiles: DownloadedFiles = DownloadedFiles(),
    val kernelUrl: String = Constants.DEF_URL_KERNEL,
    val openclashUrl: String = Constants.DEF_URL_OPENCLASH,
    val fanUrl: String = Constants.DEF_URL_FAN,
    val logEntries: List<LogEntry> = emptyList(),
    val busy: Boolean = false,
    val activeOp: OpId? = null,
    val currentProgress: String? = null,
    val settingsOpen: Boolean = false,
    val aboutOpen: Boolean = false,
    val confirmAbortOpen: Boolean = false,
    val operationOpen: Boolean = false,
    val operationTitle: String = "",
)
