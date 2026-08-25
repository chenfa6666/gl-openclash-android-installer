package com.chenfa.openclashinstaller.core

import java.io.File

/**
 * 派生路径：基于 app filesDir 给出本地 kernel/ipk/fan 文件位置。
 * 等价 Windows 版 g_exeDir + FILE_*。
 */
class AppConfig(private val filesDir: File) {
    val localKernel: File get() = File(filesDir, Constants.KERNEL_FILE)
    val localIpk: File get() = File(filesDir, Constants.OPENCLASH_IPK_DEFAULT)
    val localFan: File get() = File(filesDir, Constants.FAN_IPK_FILE)

    /** 通配查找 luci-app-openclash_*_all.ipk（用户可能下了不同版本号）；等价 Windows FindLocalIpk。 */
    fun findLocalIpk(): File? = findFileByGlob(Constants.OPENCLASH_IPK_GLOB)

    /** 通用 glob 查找：* 任意字符，按文件名匹配。当找不到时返回 null。 */
    fun findFileByGlob(glob: String): File? {
        if (!filesDir.isDirectory) return null
        val re = Regex(Regex.escape(glob).replace("\\*", ".*"))
        return filesDir.listFiles()?.firstOrNull { it.isFile && re.matches(it.name) }
    }

    /** 精确文件名是否存在。 */
    fun findFileExact(name: String): File? {
        val f = File(filesDir, name)
        return if (f.isFile) f.absoluteFile else null
    }
}
