package com.chenfa.openclashinstaller.core.ext

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * 协程取消检查：在长任务循环中调用，被 cancel 时立即抛 CancellationException。
 * 等价 Windows 版 if (g_abortRequested) break。
 */
suspend fun ensureActiveOrCancel() {
    coroutineContext.ensureActive()
}
