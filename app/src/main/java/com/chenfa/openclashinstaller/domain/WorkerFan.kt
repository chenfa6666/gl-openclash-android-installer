package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ensureActiveOrCancel
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.Downloader
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 安装风扇控制（GL.iNet 专属功能）= 3 步：
 *
 * 步骤 1/3：检查本地 gl-fanctrl_0.1.3_all.ipk，不在就从 DEF_URL_FAN 下载
 *
 * 步骤 2/3：SCP 推送 /tmp/gl-fanctrl_0.1.3_all.ipk
 *
 * 步骤 3/3：opkg install + 严格校验
 *   · opkg install --force-depends --force-overwrite --force-signature /tmp/gl-fanctrl_*.ipk
 *   · echo ===VERIFY===; opkg list-installed
 *   · 校验：===VERIFY=== 之后出现 "gl-fanctrl - "（list-installed 标准前缀）且 exit==0
 *   · 失败：逐行抄 Collected errors 原文
 */
object WorkerFan {

    suspend fun execute(
        ssh: SshClient,
        downloader: Downloader,
        appConfig: AppConfig,
        fields: ConnFields,
        fanUrl: String,
        onLog: suspend (String) -> Unit,
        onProgress: suspend (String) -> Unit,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val f = fields
            ensureActiveOrCancel()

            // --- 步骤 1/3：本地检查 → 下载 ---
            onLog("==================== 步骤 1/3：准备 gl-fanctrl ipk ====================")
            val localPath = appConfig.findFileExact(Constants.FAN_IPK_FILE)?.absolutePath
                ?: appConfig.filesDir.resolve(Constants.FAN_IPK_FILE).absolutePath
            val localFile = File(localPath)
            if (!localFile.exists()) {
                onLog("· 本地未检测到 ${Constants.FAN_IPK_FILE}，开始从 GitHub 下载")
                onLog("· 下载地址: $fanUrl")
                WorkerDownload.execute(downloader, fanUrl, localPath, "gl-fanctrl",
                    onLog = onLog, onProgress = onProgress)
                if (!File(localPath).exists()) {
                    onLog("✗ 下载后文件不存在：$localPath")
                    return@withContext Result.success(false)
                }
            } else {
                onLog("✓ 使用已存在的 ${Constants.FAN_IPK_FILE}（${formatSize(localFile.length())}）")
            }
            ensureActiveOrCancel()

            // --- 步骤 2/3：SCP 推送 ---
            onLog("==================== 步骤 2/3：SCP 推送 ipk 到 /tmp/ ====================")
            ssh.connect(f.user, f.ip, f.port, f.password)
                .onFailure { onLog("✗ SSH 连接失败：${it.message ?: it.javaClass.simpleName}"); return@withContext Result.success(false) }
            val remote = "/tmp/${Constants.FAN_IPK_FILE}"
            onLog("· 推送 ${Constants.FAN_IPK_FILE} → $remote")
            ssh.scpUpload(localPath, remote)
                .onFailure {
                    onLog("✗ SCP 推送失败：${it.message ?: it.javaClass.simpleName}")
                    runCatching { ssh.disconnect() }
                    return@withContext Result.success(false)
                }
            onLog("✓ 推送完成")
            runCatching { ssh.disconnect() }
            ensureActiveOrCancel()

            // --- 步骤 3/3：opkg 安装 + 严格校验 ---
            onLog("==================== 步骤 3/3：opkg 安装 + 校验 ====================")
            val installCmd = """opkg install --force-depends --force-overwrite --force-signature $remote; echo ===VERIFY===; opkg list-installed"""
            ssh.connect(f.user, f.ip, f.port, f.password)
                .onFailure { onLog("✗ SSH 连接失败：${it.message ?: it.javaClass.simpleName}"); return@withContext Result.success(false) }
            val (code, cap) = ssh.execCommand(installCmd, timeoutMs = 60_000L).getOrElse { (-1 to it.message.orEmpty()) }
            runCatching { ssh.disconnect() }
            val verIdx = cap.indexOf("===VERIFY===")
            val after = if (verIdx < 0) cap else cap.substring(verIdx)
            val installed = after.contains("gl-fanctrl - ")
            if (!installed || code != 0) {
                onLog("✗ opkg 安装 gl-fanctrl 失败（exit=$code）")
                onLog("----- 原始输出（节选前 80 行）-----")
                cap.lineSequence().take(80).forEach { onLog("  $it") }
                return@withContext Result.success(false)
            }
            onLog("✓ gl-fanctrl 安装完成（已校验 list-installed 条目存在，exit=0）")
            onLog("提示：路由器管理 → 系统 → 启动项 / GL-iNet 面板中查看风扇控制是否出现。")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 已强制结束风扇控制安装")
            Result.success(false)
        } catch (e: Throwable) {
            onLog("✗ 风扇控制安装异常：${e.message ?: e.javaClass.simpleName}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
