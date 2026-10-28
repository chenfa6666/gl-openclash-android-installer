package com.chenfa.openclashinstaller.core

import java.io.File

/**
 * 派生路径：基于 app filesDir 给出本地 kernel/ipk/fan 文件位置。
 * 等价 Windows 版 g_exeDir + FILE_*。
 */
class AppConfig(private val filesDir: File) {
    /** filesDir 公开引用，worker 里拼接派生路径时用（filesDir 本身是构造 private）。 */
    val rootDir: File get() = filesDir

    val localKernel: File get() = File(filesDir, Constants.KERNEL_FILE)
    val localIpk: File get() = File(filesDir, Constants.OPENCLASH_IPK_DEFAULT)
    val localFan: File get() = File(filesDir, Constants.FAN_IPK_FILE)

    /**
     * 通配查找 luci-app-openclash_*_all.ipk（用户可能下了不同版本号）。
     * 多个版本共存时取最近修改的一个，避免安装到旧包。
     */
    fun findLocalIpk(): File? = localIpk.takeIf { it.isFile }
        ?: findFilesByGlob(Constants.OPENCLASH_IPK_GLOB).maxByOrNull { it.lastModified() }

    /** 通用 glob 查找：* 任意字符，按文件名匹配。当找不到时返回 null。 */
    fun findFileByGlob(glob: String): File? = findFilesByGlob(glob).firstOrNull()

    /** 通用 glob 查找，返回全部匹配文件。 */
    fun findFilesByGlob(glob: String): List<File> {
        if (!filesDir.isDirectory) return emptyList()
        // 按 '*' 分段，每段单独 Regex.escape，再用 ".*" 拼接，避免把通配符转义成字面量。
        val segments = glob.split('*')
        val regexStr = buildString {
            append('^')
            segments.forEachIndexed { i, seg ->
                append(Regex.escape(seg))
                if (i < segments.lastIndex) append(".*")
            }
            append('$')
        }
        val re = Regex(regexStr)
        return filesDir.listFiles()
            ?.filter { it.isFile && re.matches(it.name) }
            .orEmpty()
    }

    /** 精确文件名是否存在。 */
    fun findFileExact(name: String): File? {
        val f = File(filesDir, name)
        return if (f.isFile) f.absoluteFile else null
    }
}
