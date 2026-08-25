package com.chenfa.openclashinstaller.data.model

/** 本地下载文件就绪状态。 */
data class DownloadedFiles(
    val kernelReady: Boolean = false,
    val ipkReady: Boolean = false,
    val fanReady: Boolean = false,
)
