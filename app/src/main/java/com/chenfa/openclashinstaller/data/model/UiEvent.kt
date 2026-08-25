package com.chenfa.openclashinstaller.data.model

/** 一次性 UI 事件：用 Channel 投递，避免 StateFlow 重放。 */
sealed class UiEvent {
    data class Toast(val msg: String) : UiEvent()
    data class LaunchExportLog(val uri: android.net.Uri) : UiEvent()
    object OpenSettings : UiEvent()
    object CloseSettings : UiEvent()
    object OpenAbout : UiEvent()
    object CloseAbout : UiEvent()
    object OpenConfirmAbort : UiEvent()
    object CloseConfirmAbort : UiEvent()
}
