package com.chenfa.openclashinstaller.domain

import android.content.Context
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.core.ext.containsCi
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
 * 原理：GL.iNet OpenWrt 的 RNDIS 内核模块跟 OPPO Reno5 Pro 不兼容，
 * 手机插 USB 共享网络时识别不到。替换为修复版 kmod 后即可正常。
 *
 * 流程（2 步）：
 *  1. 从 app assets 提取内置 ipk → filesDir → SCP 上传到 /tmp/
 *  2. SSH 执行 opkg update + opkg install --force-reinstall /tmp/xxx.ipk
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
            val port = fields.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            // ---- 步骤 1/2：提取 assets 内置 ipk → filesDir → SCP 上传 ----
            onLog(" 步骤 1/2：加载修复 ipk ")
            val localFile = File(filesDir, Constants.USB_TETHERING_IPK)
            if (!localFile.isFile) {
                onLog("· 从内置 assets 提取 ${Constants.USB_TETHERING_IPK}")
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
                onLog("✓ 已提取到 ${localFile.absolutePath}（${localFile.length()} bytes）")
            } else {
                onLog("✓ 本地已存在 ${Constants.USB_TETHERING_IPK}（${localFile.length()} bytes）")
            }
            ensureActiveOrCancel()

            // SCP 上传
            val remotePath = "/tmp/${Constants.USB_TETHERING_IPK}"
            onLog("· SCP 上传到 $remotePath")
            val conn = ssh.connect(fields.user, fields.ip, port, fields.password)
            if (conn.isFailure) {
                onLog("✗ SSH 连接失败：${conn.exceptionOrNull()?.message ?: conn.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            val scpResult = ssh.scpUpload(localFile.absolutePath, remotePath)
            if (scpResult.isFailure) {
                onLog("✗ SCP 上传失败：${scpResult.exceptionOrNull()?.message ?: "未知错误"}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            onLog("✓ SCP 上传完成")
            ensureActiveOrCancel()

            // ---- 步骤 2/2：opkg update + opkg install --force-reinstall ----
            onLog(" 步骤 2/2：安装修复模块 ")
            val cmd = "opkg update && opkg install --force-reinstall $remotePath; echo ===VERIFY===; opkg list-installed | grep kmod-usb-net-rndis"
            onLog("· $cmd")
            // opkg update + install 在 GL.iNet 上经常需要 2-3 分钟（拉 Packages.gz 索引慢）
            val (code, out) = ssh.execCommand(cmd, timeoutMs = 180_000L).getOrElse {
                (-1 to (it.message.orEmpty()))
            }
            runCatching { ssh.disconnect() }

            onLog("· exit=$code")
            out.lineSequence().take(60).forEach { onLog("  $it") }

            // 校验：VERIFY 后必须出现 kmod-usb-net-rndis 前缀 + exit==0
            val verIdx = out.indexOf("===VERIFY===")
            val after = if (verIdx < 0) out else out.substring(verIdx)
            val installed = after.containsCi("kmod-usb-net-rndis")
            if (!installed || code != 0) {
                onLog("✗ 安装失败（kmod-usb-net-rndis 未出现在 list-installed 或 exit=$code）")
                out.lineSequence().take(80).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            onLog("✓ kmod-usb-net-rndis 已安装，修复完成")
            onLog(" 修复成功 ")
            onLog("提示：重启路由器，OPPO Reno5 Pro USB 共享网络应恢复正常。")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 已强制结束 USB 共享网络修复")
            Result.success(false)
        } catch (e: Throwable) {
            onLog("✗ 修复异常：${e.message ?: e.javaClass.simpleName}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
