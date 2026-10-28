package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.Downloader
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 安装风扇控制（GL.iNet 专属功能），共 3 步：
 *
 * 1/3 准备本地 gl-fanctrl ipk，缺失则下载
 * 2/3 SCP 推送到 /tmp/
 * 3/3 opkg 安装并用 list-installed 严格校验
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
    ): Result<Boolean> = withContext(Dispatchers.IO) fan@{
        try {
            val f = fields.normalized()
            val port = f.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            // --- 1/3：准备 ipk ---
            onLog("步骤 1/3 · 准备风扇控制插件")
            val localPath = appConfig.findFileExact(Constants.FAN_IPK_FILE)?.absolutePath
                ?: appConfig.rootDir.resolve(Constants.FAN_IPK_FILE).absolutePath
            val localFile = File(localPath)
            if (!localFile.exists()) {
                onLog("· 本地未找到 ${Constants.FAN_IPK_FILE}，开始下载")
                val ok = WorkerDownload(downloader, onLog, onProgress).execute(fanUrl, localPath, "风扇控制插件")
                ensureActiveOrCancel()
                if (!ok || !File(localPath).exists()) {
                    onLog("✗ 插件下载失败，无法继续安装")
                    return@fan Result.success(false)
                }
            } else {
                onLog("✓ 使用本地插件（${formatBytes(localFile.length())}）")
            }
            ensureActiveOrCancel()

            // --- 2/3：SCP 推送 ---
            onLog("步骤 2/3 · 上传插件到 /tmp")
            val conn1 = ssh.connect(f.user, f.ip, port, f.password)
            if (conn1.isFailure) {
                onLog("✗ SSH 连接失败：${conn1.exceptionOrNull()?.toUserMessage("请检查 IP、端口和 root 密码")}")
                return@fan Result.success(false)
            }
            val remote = "/tmp/${Constants.FAN_IPK_FILE}"
            onLog("· 上传 ${Constants.FAN_IPK_FILE}（${formatBytes(localFile.length())}）")
            val up = ssh.scpUpload(localPath, remote)
            if (up.isFailure) {
                onLog("✗ 上传失败：${up.exceptionOrNull()?.toUserMessage("请检查路由器 /tmp 空间")}")
                runCatching { ssh.disconnect() }
                return@fan Result.success(false)
            }
            onLog("✓ 插件已上传")
            runCatching { ssh.disconnect() }
            ensureActiveOrCancel()

            // --- 3/3：opkg 安装 + 校验 ---
            onLog("步骤 3/3 · 安装并校验")
            val installCmd =
                """opkg install --force-depends --force-overwrite --force-signature $remote; echo ===VERIFY===; opkg list-installed"""
            val conn2 = ssh.connect(f.user, f.ip, port, f.password)
            if (conn2.isFailure) {
                onLog("✗ SSH 连接失败：${conn2.exceptionOrNull()?.toUserMessage("请检查 IP、端口和 root 密码")}")
                return@fan Result.success(false)
            }
            val (code, cap) = ssh.execCommand(installCmd, timeoutMs = 180_000L).getOrElse { e ->
                onLog("✗ 安装命令执行失败：${e.toUserMessage("请稍后重试")}")
                runCatching { ssh.disconnect() }
                return@fan Result.success(false)
            }
            runCatching { ssh.disconnect() }
            val verIdx = cap.indexOf("===VERIFY===")
            if (verIdx < 0) {
                onLog("✗ 未收到安装校验标记，无法确认插件是否安装成功")
                onLog("--- opkg 输出（前 60 行）---")
                cap.lineSequence().take(60).forEach { onLog("  $it") }
                return@fan Result.success(false)
            }
            val after = cap.substring(verIdx)
            val installed = after.contains("gl-fanctrl - ")
            if (!installed || code != 0) {
                onLog("✗ 风扇控制安装失败（exit=$code）")
                onLog("--- opkg 输出（前 60 行）---")
                cap.lineSequence().take(60).forEach { onLog("  $it") }
                return@fan Result.success(false)
            }
            onLog("✓ 风扇控制安装完成（已校验安装记录，exit=0）")
            onLog("· 查看位置：GL 管理界面 → 系统 → 风扇控制")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 风扇控制安装已取消")
            throw ce
        } catch (e: Throwable) {
            onLog("✗ 风扇控制安装异常：${e.toUserMessage("请稍后重试")}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
