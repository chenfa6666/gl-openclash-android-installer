package com.chenfa.openclashinstaller.data.model

/** 任务结束状态。 */
enum class OpStatus {
    SUCCESS,
    FAILED,
    CANCELLED,
}

/**
 * 一次任务的最终结果。
 *
 * 保留在 UiState 中：关闭日志弹窗后，安装页仍会显示结果卡片，
 * 用户可以查看结论、耗时，并在失败时一键重试。
 */
data class OpOutcome(
    val op: OpId,
    val title: String,
    val status: OpStatus,
    val summary: String,
    val startedAt: Long,
    val finishedAt: Long,
) {
    val success: Boolean get() = status == OpStatus.SUCCESS
    val durationMs: Long get() = (finishedAt - startedAt).coerceAtLeast(0L)
}
