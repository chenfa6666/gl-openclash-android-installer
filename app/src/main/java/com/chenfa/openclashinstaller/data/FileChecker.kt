package com.chenfa.openclashinstaller.data

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.data.model.DownloadedFiles
import com.chenfa.openclashinstaller.data.model.EnvItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 环境检查：检查 SSH 客户端与本地安装包是否就绪。
 *
 * Android 上 SSH 由 JSch 库内置，所以 SSH 行恒为就绪，额外显示文件名与大小，
 * 让用户在点「开始安装」前就能确认使用的是哪个版本的安装包。
 */
class FileChecker(private val appConfig: AppConfig) {

    suspend fun check(): List<EnvItem> = withContext(Dispatchers.IO) {
        val kernel = appConfig.localKernel
        val ipk = appConfig.findLocalIpk()
        val fan = appConfig.localFan

        listOf(
            EnvItem(
                name = "SSH 客户端",
                ok = true,
                detail = "JSch 内置，无需额外安装",
            ),
            EnvItem(
                name = "Clash 内核",
                ok = kernel.isFile,
                detail = fileDetail(kernel) ?: "未下载，将安装时自动下载",
            ),
            EnvItem(
                name = "OpenClash 插件",
                ok = ipk != null,
                detail = ipk?.let(::fileDetail) ?: "未下载，将安装时自动下载",
            ),
            EnvItem(
                name = "风扇控制插件",
                ok = fan.isFile,
                detail = fileDetail(fan) ?: "未下载，安装风扇时自动下载",
            ),
        )
    }

    /** 获取就绪状态。 */
    suspend fun downloadedFiles(): DownloadedFiles = withContext(Dispatchers.IO) {
        val kernel = appConfig.localKernel
        val ipk = appConfig.findLocalIpk()
        val fan = appConfig.localFan
        DownloadedFiles(
            kernelReady = kernel.isFile,
            ipkReady = ipk != null,
            fanReady = fan.isFile,
            kernelName = kernel.takeIf { it.isFile }?.name,
            kernelBytes = kernel.takeIf { it.isFile }?.length() ?: 0L,
            ipkName = ipk?.name,
            ipkBytes = ipk?.length() ?: 0L,
            fanName = fan.takeIf { it.isFile }?.name,
            fanBytes = fan.takeIf { it.isFile }?.length() ?: 0L,
        )
    }

    private fun fileDetail(file: File?): String? {
        if (file == null || !file.isFile) return null
        return "${file.name} · ${formatBytes(file.length())}"
    }
}
