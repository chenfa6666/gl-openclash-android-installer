package com.chenfa.openclashinstaller.data.model

/** 本地安装包就绪状态与文件信息。 */
data class DownloadedFiles(
    val kernelReady: Boolean = false,
    val ipkReady: Boolean = false,
    val fanReady: Boolean = false,
    val kernelName: String? = null,
    val kernelBytes: Long = 0L,
    val ipkName: String? = null,
    val ipkBytes: Long = 0L,
    val fanName: String? = null,
    val fanBytes: Long = 0L,
)
