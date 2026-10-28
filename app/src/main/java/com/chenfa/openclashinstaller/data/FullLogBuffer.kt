package com.chenfa.openclashinstaller.data

/**
 * 完整日志缓冲（线程安全）。
 *
 * 取消任务时需要在已取消的协程里同步写入「操作已取消」，
 * 因此这里统一用一个对象锁，避免 suspend Mutex 与 synchronized 两套锁混用。
 */
class FullLogBuffer {
    private val builder = StringBuilder()
    private val lock = Any()

    fun append(line: String) {
        synchronized(lock) {
            builder.append(line)
            if (line.isNotEmpty() && !line.endsWith('\n')) builder.append('\n')
        }
    }

    fun appendSync(line: String) = append(line)

    fun snapshot(): String = synchronized(lock) { builder.toString() }

    fun snapshotSync(): String = snapshot()

    fun clear() {
        synchronized(lock) { builder.setLength(0) }
    }
}
