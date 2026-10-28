package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.containsCi
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
 * 4 步安装 OpenClash：
 *
 * 1/4  SSH 安装 opkg 依赖（失败软警告，后续 --force-depends 兜底）
 * 2/4  SCP 推送内核与 ipk 到 /tmp/
 * 3/4  opkg 安装插件，并用 list-installed 严格校验
 * 4/4  解压内核到 /etc/openclash/core/clash_meta 并校验
 *
 * 日志遵循统一约定：`步骤 x/y`、`·` 详情、`✓` 成功、`⚠` 警告、`✗` 失败。
 */
object WorkerInstall {

    suspend fun execute(
        ssh: SshClient,
        downloader: Downloader,
        appConfig: AppConfig,
        fields: ConnFields,
        urls: Triple<String, String, String>, // kernelUrl, openclashUrl, fanUrl (fan 不用)
        onLog: suspend (String) -> Unit,
        onProgress: suspend (String) -> Unit,
    ): Result<Boolean> = withContext(Dispatchers.IO) worker@{
        try {
            val (kernelUrl, openclashUrl, _) = urls
            val f = fields.normalized()
            val port = f.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            // 本地文件兜底：不在则先下载
            val kernelLocal = appConfig.findFileExact(Constants.KERNEL_FILE)?.absolutePath
                ?: appConfig.rootDir.resolve(Constants.KERNEL_FILE).absolutePath
            if (!File(kernelLocal).exists()) {
                onLog("· 本地未找到 Clash 内核，先下载")
                val ok = WorkerDownload(downloader, onLog, onProgress).execute(kernelUrl, kernelLocal, "Clash 内核")
                ensureActiveOrCancel()
                if (!ok) return@worker Result.success(false)
            }
            val ipkLocal = appConfig.findLocalIpk()?.absolutePath
                ?: appConfig.rootDir.resolve(Constants.OPENCLASH_IPK_DEFAULT).absolutePath
            if (!File(ipkLocal).exists()) {
                onLog("· 本地未找到 OpenClash 插件，先下载")
                val ok = WorkerDownload(downloader, onLog, onProgress).execute(openclashUrl, ipkLocal, "OpenClash 插件")
                ensureActiveOrCancel()
                if (!ok) return@worker Result.success(false)
            }

            // --- 1/4：SSH 装依赖 ---
            onLog("步骤 1/4 · 安装 opkg 依赖")
            onLog("· 更新软件源并安装 ${Constants.OPKG_DEPS.size} 个依赖包")
            val depCmd = buildString {
                append("opkg update")
                Constants.OPKG_DEPS.forEach { append(" && opkg install ").append(it) }
            }
            if (!connect(ssh, f, port, onLog)) return@worker Result.success(false)
            val r = ssh.execCommand(depCmd, timeoutMs = 180_000L).getOrElse { e ->
                onLog("✗ 依赖安装命令执行失败：${e.toUserMessage("请检查路由器网络与软件源")}")
                return@worker Result.success(false)
            }
            val code = r.first
            val cap = r.second
            if (code != 0 || !cap.containsCi(Constants.OPKG_DEPS_LAST)) {
                onLog("⚠ 依赖安装未完全成功（exit=$code），继续安装时会启用 --force-depends 兜底")
                cap.lineSequence()
                    .filter {
                        it.contains("error", ignoreCase = true) ||
                            it.contains("fail", ignoreCase = true) ||
                            it.startsWith(" *")
                    }
                    .take(12)
                    .forEach { onLog("  $it") }
            } else {
                onLog("✓ 依赖已就绪（${Constants.OPKG_DEPS.size} 个包）")
            }
            ensureActiveOrCancel()
            runCatching { ssh.disconnect() }

            // --- 2/4：SCP 推送 ---
            onLog("步骤 2/4 · 上传安装包到 /tmp")
            if (!connect(ssh, f, port, onLog)) return@worker Result.success(false)
            val pairs = listOf(
                kernelLocal to Constants.KERNEL_FILE,
                ipkLocal to File(ipkLocal).name,
            )
            for ((local, remoteName) in pairs) {
                ensureActiveOrCancel()
                val remotePath = "/tmp/$remoteName"
                onLog("· 上传 $remoteName（${formatBytes(File(local).length())}）→ $remotePath")
                val up = ssh.scpUpload(local, remotePath)
                if (up.isFailure) {
                    val message = up.exceptionOrNull()?.toUserMessage("请检查路由器 /tmp 空间是否充足")
                        ?: "未知错误"
                    onLog("✗ 上传 $remoteName 失败：$message")
                    runCatching { ssh.disconnect() }
                    return@worker Result.success(false)
                }
                onLog("✓ $remoteName 已上传")
            }
            runCatching { ssh.disconnect() }

            // --- 3/4：opkg 安装 + 严格校验 ---
            onLog("步骤 3/4 · 安装 OpenClash 插件")
            val ipkRemoteName = File(ipkLocal).name
            onLog("· 安装 /tmp/$ipkRemoteName")
            val installCmd =
                """opkg install --force-depends --force-overwrite --force-signature /tmp/$ipkRemoteName; echo ===VERIFY===; opkg list-installed"""
            if (!connect(ssh, f, port, onLog)) return@worker Result.success(false)
            val (code3, cap3) = ssh.execCommand(installCmd, timeoutMs = 180_000L).getOrElse { e ->
                onLog("✗ 插件安装命令执行失败：${e.toUserMessage("请稍后重试")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            runCatching { ssh.disconnect() }
            val verIdx = cap3.indexOf("===VERIFY===")
            if (verIdx < 0) {
                onLog("✗ 未收到安装校验标记，无法确认插件是否安装成功")
                onLog("--- opkg 输出（前 60 行）---")
                cap3.lineSequence().take(60).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            val after = cap3.substring(verIdx)
            val installed = after.contains("luci-app-openclash - ")
            if (!installed || code3 != 0) {
                onLog("✗ 插件安装失败（exit=$code3，list-installed 中未找到 luci-app-openclash）")
                onLog("--- opkg 输出（前 60 行）---")
                cap3.lineSequence().take(60).forEach { onLog("  $it") }
                onLog("· 建议：登录路由器执行 opkg update，确认软件源可达后重试")
                return@worker Result.success(false)
            }
            onLog("✓ 插件安装完成（已校验安装记录，exit=0）")
            ensureActiveOrCancel()

            // --- 4/4：解压内核 ---
            onLog("步骤 4/4 · 部署 Clash 内核")
            onLog("· 解压到 /etc/openclash/core/clash_meta")
            val kernelCmd =
                """mkdir -p /etc/openclash/core && tar -xzf /tmp/${Constants.KERNEL_FILE} -O > /etc/openclash/core/clash_meta && chmod +x /etc/openclash/core/clash_meta && ls -la /etc/openclash/core/clash_meta && echo KERNEL_OK"""
            if (!connect(ssh, f, port, onLog)) return@worker Result.success(false)
            val (code4, cap4) = ssh.execCommand(kernelCmd, timeoutMs = 30_000L).getOrElse { e ->
                onLog("✗ 内核部署命令执行失败：${e.toUserMessage("请稍后重试")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            runCatching { ssh.disconnect() }
            if (!cap4.contains("KERNEL_OK") || code4 != 0) {
                onLog("✗ 内核解压失败（exit=$code4），请确认内核为 arm64 架构")
                onLog("--- 原始输出（前 40 行）---")
                cap4.lineSequence().take(40).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            val lsLine = cap4.lineSequence().firstOrNull { it.contains("clash_meta") && it.contains("-rwx") }
            onLog("✓ 内核已部署${lsLine?.let { " · $it" } ?: ""}")

            onLog("✓ 安装完成")
            onLog("· 管理后台：http://${f.ip}/")
            onLog("· 下一步：登录后台 → 服务 → OpenClash，完成订阅与启动")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 安装已取消")
            throw ce
        } catch (e: Throwable) {
            onLog("✗ 安装异常：${e.toUserMessage("请稍后重试")}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    private suspend fun connect(
        ssh: SshClient,
        fields: ConnFields,
        port: Int,
        onLog: suspend (String) -> Unit,
    ): Boolean {
        val result = ssh.connect(fields.user, fields.ip, port, fields.password)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.toUserMessage("请检查 IP、端口和 root 密码")
                ?: "未知错误"
            onLog("✗ SSH 连接失败：$message")
            return false
        }
        return true
    }
}
