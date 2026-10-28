package com.chenfa.openclashinstaller.data.model

/** 一次性 UI 事件：用 Channel 投递，避免 StateFlow 重放。 */
sealed class UiEvent {
    /** 底部提示条。 */
    data class Notice(
        val message: String,
        val level: NoticeLevel = NoticeLevel.INFO,
    ) : UiEvent()

    /** 设置已保存：主界面切回安装页。 */
    data object SettingsSaved : UiEvent()
}
