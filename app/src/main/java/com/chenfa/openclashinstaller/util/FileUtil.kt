package com.chenfa.openclashinstaller.util

import java.io.OutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * 文件工具：UTF-8 BOM 写出（等价 Windows 版 IDM_EXPORTLOG 的 CreateFileW + 写 BOM）。
 * 注意：Android 10+ 用 SAF (ACTION_CREATE_DOCUMENT) 拿 Uri 后通过 ContentResolver.openOutputStream。
 */
object FileUtil {

    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    /** 写 UTF-8 BOM + 文本到 OutputStream。 */
    fun writeUtf8Bom(stream: OutputStream, text: String) {
        stream.write(BOM)
        stream.write(text.toByteArray(StandardCharsets.UTF_8))
        stream.flush()
    }

    /** 写 UTF-8 BOM + 文本到本地文件。 */
    fun writeUtf8BomFile(path: File, text: String) {
        path.outputStream().use { writeUtf8Bom(it, text) }
    }

    /** 转 UTF-8 字节数组。等价 Windows 版 WideToUtf8。 */
    fun toUtf8(text: String): ByteArray = text.toByteArray(StandardCharsets.UTF_8)
}
