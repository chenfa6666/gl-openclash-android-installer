package com.chenfa.openclashinstaller.data.model

/**
 * 日志条目种类：用于 UI 着色与排版。
 *
 * 约定：
 *  - 以 `✓` 开头的行 → SUCCESS
 *  - 以 `✗` 开头的行 → ERROR
 *  - 以 `⚠` / `!` 开头的行 → WARN
 *  - 以 `步骤 1/4` 开头的行 → STEP
 *  - 以 `---` 包裹的行 → SECTION
 *  - 其余 → NORMAL（原始输出等）
 *
 * ViewModel 会根据行首符号自动归类，worker 不需要显式传 kind。
 */
enum class LogKind {
    NORMAL,    // 普通信息 / 原始输出
    STEP,      // 流程步骤标题
    SECTION,   // 分隔标题（如“原始输出”）
    PROGRESS,  // 进度行（同 key 覆盖）
    SUCCESS,   // ✓ 绿
    ERROR,     // ✗ 红
    WARN,      // ! 黄
}
