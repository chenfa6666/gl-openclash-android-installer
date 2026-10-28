package com.chenfa.openclashinstaller.domain

import android.content.Context
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.containsCi
import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 修复 OPPO Reno5 Pro USB 共享网络 bug。
 *
 * GL.iNet OpenWrt 自带 RNDIS 内核模块与 OPPO Reno5 Pro 不兼容，
 * 替换为内置的修复版 kmod 后即可正常识别 USB 网络。
 */
object WorkerUsbTethering {

    suspend fun execute(
        ssh: SshClient,
        fields: ConnFields,
        appContext: Context,
        filesDir: File,
        onLog: suspend (String) -> Unit,
    ): Result<Boolean> = withContext(Dispatchers.IO) worker@{
        try {
            val f = fields.normalized()
            val port = f.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            // ---- 1/2：提取内置 ipk 并上传 ----
            onLog("步骤 1/2 · 准备修复模块")
            val localFile = File(filesDir, Constants.USB_TETHERING_IPK)
            if (!localFile.isFile) {
                onLog("· 从应用内置资源提取修复模块")
                appContext.assets.open(Constants.USB_TETHERING_IPK).use { input ->
                    FileOutputStream(localFile).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                        }
                    }
                }
            }
            onLog("✓ 修复模块已就绪（${formatBytes(localFile.length())}）")
            ensureActiveOrCancel()

            val remotePath = "/tmp/${Constants.USB_TETHERING_IPK}"
            val conn = ssh.connect(f.user, f.ip, port, f.password)
            if (conn.isFailure) {
                onLog("✗ SSH 连接失败：${conn.exceptionOrNull()?.toUserMessage("请检查 IP、端口和 root 密码")}")
                return@worker Result.success(false)
            }
            onLog("· 上传修复模块到 /tmp")
            val scpResult = ssh.scpUpload(localFile.absolutePath, remotePath)
            if (scpResult.isFailure) {
                onLog("✗ 上传失败：${scpResult.exceptionOrNull()?.toUserMessage("请检查路由器 /tmp 空间")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            onLog("✓ 修复模块已上传")
            ensureActiveOrCancel()

            // ---- 2/2：安装并校验 ----
            onLog("步骤 2/2 · 安装修复模块")
            val cmd =
                "opkg update && opkg install --force-reinstall $remotePath; echo ===VERIFY===; opkg list-installed | grep kmod-usb-net-rndis"
            val (code, out) = ssh.execCommand(cmd, timeoutMs = 180_000L).getOrElse { e ->
                onLog("✗ 安装命令执行失败：${e.toUserMessage("请稍后重试")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            runCatching { ssh.disconnect() }

            val verIdx = out.indexOf("===VERIFY===")
            if (verIdx < 0) {
                onLog("✗ 未收到安装校验标记，无法确认修复模块是否安装成功")
                out.lineSequence().filter { it.isNotBlank() }.take(40).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            val after = out.substring(verIdx)
            val installed = after.containsCi("kmod-usb-net-rndis")
            if (!installed || code != 0) {
                onLog("✗ 修复模块安装失败（exit=$code）")
                onLog("--- opkg 输出（前 60 行）---")
                out.lineSequence().filter { it.isNotBlank() }.take(60).forEach { onLog("  $it") }
                onLog("· 建议：确认路由器可以访问软件源后重试")
                return@worker Result.success(false)
            }
            onLog("✓ 修复模块已安装")
            onLog("· 请重启路由器，OPPO Reno5 Pro 的 USB 共享网络即可恢复")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! USB 共享网络修复已取消")
            throw ce
        } catch (e: Throwable) {
            onLog("✗ 修复异常：${e.toUserMessage("请稍后重试")}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
