package com.chenfa.openclashinstaller.data

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.data.model.EnvItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 环境检查：等价 Windows 版 RefreshEnvCheck()。
 *
 * Windows 版检查系统 ssh.exe 是否存在；Android 上 SSH 由 JSch 库内置，
 * 所以 SSH 行恒为 yes（标签改为"JSch 库就绪"）。
 */
class FileChecker(private val appConfig: AppConfig) {

    suspend fun check(): List<EnvItem> = withContext(Dispatchers.IO) {
        listOf(
            EnvItem("JSch 库就绪", ok = true),
            EnvItem("clash-linux-arm64.tar.gz", ok = appConfig.localKernel.exists()),
            EnvItem("luci-app-openclash_*.ipk", ok = (appConfig.findLocalIpk() != null)),
            EnvItem("gl-fanctrl_0.1.3_all.ipk", ok = appConfig.localFan.exists()),
        )
    }

    /** 获取就绪状态。 */
    suspend fun downloadedFiles(): com.chenfa.openclashinstaller.data.model.DownloadedFiles =
        withContext(Dispatchers.IO) {
            com.chenfa.openclashinstaller.data.model.DownloadedFiles(
                kernelReady = appConfig.localKernel.exists(),
                ipkReady = appConfig.findLocalIpk() != null,
                fanReady = appConfig.localFan.exists(),
            )
        }
}
