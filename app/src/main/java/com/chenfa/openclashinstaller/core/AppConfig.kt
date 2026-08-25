package com.chenfa.openclashinstaller.core

import java.io.File

/**
 * 派生路径：基于 app filesDir 给出本地 kernel/ipk/fan 文件位置。
 * 等价 Windows 版 g_exeDir + FILE_*。
 */
class AppConfig(private val filesDir: File) {
    val localKernel: File get() = File(filesDir, Constants.FILE_KERNEL)
    val localIpk: File get() = File(filesDir, Constants.FILE_OPENCLASH)
    val localFan: File get() = File(filesDir, Constants.FILE_FAN)

    /** 通配查找 luci-app-openclash_*_all.ipk（用户可能下了不同版本号） */
    fun findLocalIpk(): File? = findFileByPattern(filesDir, "luci-app-openclash_.*_all\\.ipk".toRegex())

    companion object {
        fun findFileByPattern(dir: File, pattern: Regex): File? {
            if (!dir.isDirectory) return null
            return dir.listFiles()?.firstOrNull { it.isFile && pattern.containsMatchIn(it.name) }
        }
    }
}
