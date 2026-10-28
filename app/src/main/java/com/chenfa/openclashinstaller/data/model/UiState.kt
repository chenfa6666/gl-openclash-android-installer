package com.chenfa.openclashinstaller.data.model

import com.chenfa.openclashinstaller.core.Constants

/** UI 单一状态。 */
data class UiState(
    val envStatus: List<EnvItem> = emptyList(),
    val fields: ConnFields = ConnFields(),
    val downloadedFiles: DownloadedFiles = DownloadedFiles(),
    val kernelUrl: String = Constants.DEF_URL_KERNEL,
    val openclashUrl: String = Constants.DEF_URL_OPENCLASH,
    val fanUrl: String = Constants.DEF_URL_FAN,
    val logEntries: List<LogEntry> = emptyList(),
    val busy: Boolean = false,
    val aborting: Boolean = false,
    val activeOp: OpId? = null,
    val operationOpen: Boolean = false,
    val operationTitle: String = "",
    val operationStartedAt: Long? = null,
    val outcome: OpOutcome? = null,
    val confirmAbortOpen: Boolean = false,
    val importing: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val monetEnabled: Boolean = false,
    val blurEnabled: Boolean = false,
    val floatingBottomBar: Boolean = false,
    val glassEnabled: Boolean = true,
    val pageSwitchGesture: Boolean = true,
    val uiScale: Float = 1f,
    val connChecking: Boolean = false,
    val connChecked: Boolean = false,
    val connDeviceName: String? = null,
    val connError: String? = null,
)
