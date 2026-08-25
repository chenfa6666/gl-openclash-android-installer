package com.chenfa.openclashinstaller.data.model

/** 日志条目种类：用于 UI 着色。等价 Windows 版 wParam=0/1 + 前缀符号 ✓✗·! 的视觉区分。 */
enum class LogKind {
    NORMAL,    // 默认灰
    PROGRESS,  // 进度行（同 key 覆盖）
    SUCCESS,   // ✓ 绿
    ERROR,     // ✗ 红
    WARN,      // ! 黄
}
