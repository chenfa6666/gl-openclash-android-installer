package com.chenfa.openclashinstaller.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 完整日志缓冲（线程安全）。
 * 等价 Windows 版 g_fullLog + std::mutex g_logMutex。
 *
 * 「导出完整日志」用此缓冲，写入 UTF-8 BOM 文本。
 * 界面 LogFilter 会过滤掉部分噪音行，但完整缓冲保留所有原始字节。
 */
class FullLogBuffer {
    private val sb = StringBuilder()
    private val mutex = Mutex()

    suspend fun append(line: String) = mutex.withLock {
        sb.append(line)
        if (line.isNotEmpty() && !line.endsWith('\n')) sb.append('\n')
    }

    fun appendSync(line: String) {
        synchronized(sb) {
            sb.append(line)
            if (line.isNotEmpty() && !line.endsWith('\n')) sb.append('\n')
        }
    }

    suspend fun snapshot(): String = mutex.withLock { sb.toString() }

    fun snapshotSync(): String = synchronized(sb) { sb.toString() }

    suspend fun clear() = mutex.withLock { sb.setLength(0) }
}
