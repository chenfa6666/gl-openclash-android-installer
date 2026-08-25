package com.chenfa.openclashinstaller.core.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * 协程取消检查：如果在长任务循环中调用，被 cancel 时立即抛 CancellationException。
 * 等价 Windows 版 if (g_abortRequested) break + return。
 */
suspend fun ensureActiveOrCancel() {
    coroutineContext.ensureActive()
}

/**
 * 安全启动：若当前 busy 则忽略，避免重复启动。
 */
fun CoroutineScope.launchIfFree(busy: Boolean, block: () -> Unit): Boolean {
    if (busy) return false
    block()
    return true
}
