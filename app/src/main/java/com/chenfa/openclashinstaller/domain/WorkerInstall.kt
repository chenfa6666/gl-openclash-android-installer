package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.containsCi
import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.Downloader
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 4 步安装 OpenClash（等价 Windows 版 WorkerInstall）：
 *
 * 步骤 1/4：SSH 装依赖（opkg update + OPKG_DEPS）
 *   · 失败软警告（让 --force-depends 兜底），不中断
 *
 * 步骤 2/4：SCP 推送 2 文件到 /tmp/
 *   · scpUpload（远端 cat > '/tmp/xxx'）
 *   · 失败中断
 *
 * 步骤 3/4：opkg 装 ipk + 严格校验
 *   · opkg install --force-depends --force-overwrite --force-signature /tmp/xxx.ipk
 *   · echo ===VERIFY===; opkg list-installed | grep
 *   · 校验：===VERIFY=== 之后必须匹配 "luci-app-openclash - " 前缀 + exit==0
 *   · 失败：逐行抄 ===VERIFY=== 前后 Collected errors 原文，不误判成功
 *
 * 步骤 4/4：解压内核到 /etc/openclash/core/clash_meta + 校验
 *   · mkdir -p && tar -xzf /tmp/... -O > .../clash_meta + chmod +x + ls -la + echo KERNEL_OK
 *   · 校验：cap 含 "KERNEL_OK"
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
            val f = fields
            ensureActiveOrCancel()

            // 先确保 2 文件在本地（不在→下载）：Windows 版是主流程已下，Android 版兜底
            val kernelLocal = appConfig.findFileExact(Constants.KERNEL_FILE)?.absolutePath
                ?: appConfig.rootDir.resolve(Constants.KERNEL_FILE).absolutePath
            if (!java.io.File(kernelLocal).exists()) {
                onLog("· 内核本地未找到，开始下载")
                val ok = WorkerDownload(downloader, onLog, onProgress).execute(kernelUrl, kernelLocal, "内核")
                ensureActiveOrCancel()
                if (!ok) return@worker Result.success(false)
            }
            val ipkLocal = appConfig.findLocalIpk()?.absolutePath
                ?: appConfig.rootDir.resolve(Constants.OPENCLASH_IPK_DEFAULT).absolutePath
            if (!java.io.File(ipkLocal).exists()) {
                onLog("· openclash ipk 本地未找到，开始下载")
                val ok = WorkerDownload(downloader, onLog, onProgress).execute(openclashUrl, ipkLocal, "openclash")
                ensureActiveOrCancel()
                if (!ok) return@worker Result.success(false)
            }

            // --- 步骤 1/4：SSH 装依赖 ---
            onLog("==================== 步骤 1/4：安装 opkg 依赖 ====================")
            val depCmd = buildString {
                append("opkg update")
                Constants.OPKG_DEPS.forEach { append(" && opkg install ").append(it) }
            }
            val connDep = ssh.connect(f.user, f.ip, f.port, f.password)
            if (connDep.isFailure) {
                onLog("✗ SSH 连接失败：${connDep.exceptionOrNull()?.message ?: connDep.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            val r = ssh.execCommand(depCmd, timeoutMs = 90_000L).getOrElse { (-1 to it.message.orEmpty()) }
            val code = r.first; val cap = r.second
            if (code != 0 || !cap.containsCi(Constants.OPKG_DEPS_LAST)) {
                onLog("⚠ 依赖安装未完全成功（exit=$code），后续 opkg 将启用 --force-depends 兜底")
                cap.lineSequence().filter {
                    it.contains("error", ignoreCase = true) || it.contains("fail", ignoreCase = true) || it.startsWith(" *")
                }.take(20).forEach { onLog("  · $it") }
            } else {
                onLog("✓ 依赖安装完成（${Constants.OPKG_DEPS.size} 个包）")
            }
            ensureActiveOrCancel()
            try { ssh.disconnect() } catch (_: Throwable) {}

            // --- 步骤 2/4：SCP 推送 2 文件到 /tmp/ ---
            onLog("==================== 步骤 2/4：SCP 推送文件到 /tmp/ ====================")
            val connScp = ssh.connect(f.user, f.ip, f.port, f.password)
            if (connScp.isFailure) {
                onLog("✗ SSH 连接失败：${connScp.exceptionOrNull()?.message ?: connScp.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            val pairs = listOf(
                kernelLocal to Constants.KERNEL_FILE,
                ipkLocal to java.io.File(ipkLocal).name
            )
            for ((local, remoteName) in pairs) {
                ensureActiveOrCancel()
                val remotePath = "/tmp/$remoteName"
                onLog("· 推送 $remoteName → $remotePath")
                val up = ssh.scpUpload(local, remotePath)
                if (up.isFailure) {
                    onLog("✗ SCP 推送 $remoteName 失败：${up.exceptionOrNull()?.message ?: up.exceptionOrNull()?.javaClass?.simpleName}")
                    try { ssh.disconnect() } catch (_: Throwable) {}
                    return@worker Result.success(false)
                }
                onLog("✓ 推送完成 $remoteName")
            }
            try { ssh.disconnect() } catch (_: Throwable) {}

            // --- 步骤 3/4：opkg 装 ipk + 严格校验 ---
            onLog("==================== 步骤 3/4：opkg 安装 ipk ====================")
            val ipkRemoteName = java.io.File(ipkLocal).name
            val installCmd = """opkg install --force-depends --force-overwrite --force-signature /tmp/$ipkRemoteName; echo ===VERIFY===; opkg list-installed"""
            val conn3 = ssh.connect(f.user, f.ip, f.port, f.password)
            if (conn3.isFailure) {
                onLog("✗ SSH 连接失败：${conn3.exceptionOrNull()?.message ?: conn3.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            val (code3, cap3) = ssh.execCommand(installCmd, timeoutMs = 60_000L).getOrElse { (-1 to it.message.orEmpty()) }
            try { ssh.disconnect() } catch (_: Throwable) {}
            val verIdx = cap3.indexOf("===VERIFY===")
            val after = if (verIdx < 0) cap3 else cap3.substring(verIdx)
            // 严格校验：opkg list-installed 输出前缀是 "luci-app-openclash - 0.x.x-xxx"
            val installed = after.contains("luci-app-openclash - ")
            if (!installed || code3 != 0) {
                onLog("✗ opkg 安装失败（exit=$code3，未能在 list-installed 中找到 luci-app-openclash 条目）")
                onLog("----- opkg 原始输出（节选前 80 行）-----")
                cap3.lineSequence().take(80).forEach { onLog("  $it") }
                onLog("----- 建议：SSH 进路由器执行 opkg update && opkg install ${Constants.OPKG_DEPS.joinToString(" ")} 后重试 -----")
                return@worker Result.success(false)
            } else {
                onLog("✓ opkg 安装完成（已校验 luci-app-openclash 条目存在，exit=0）")
            }
            ensureActiveOrCancel()

            // --- 步骤 4/4：解压内核到 /etc/openclash/core/ + 校验 KERNEL_OK ---
            onLog("==================== 步骤 4/4：解压内核到 /etc/openclash/core/ ====================")
            val kernelCmd = """mkdir -p /etc/openclash/core && tar -xzf /tmp/${Constants.KERNEL_FILE} -O > /etc/openclash/core/clash_meta && chmod +x /etc/openclash/core/clash_meta && ls -la /etc/openclash/core/clash_meta && echo KERNEL_OK"""
            val conn4 = ssh.connect(f.user, f.ip, f.port, f.password)
            if (conn4.isFailure) {
                onLog("✗ SSH 连接失败：${conn4.exceptionOrNull()?.message ?: conn4.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            val (code4, cap4) = ssh.execCommand(kernelCmd, timeoutMs = 30_000L).getOrElse { (-1 to it.message.orEmpty()) }
            try { ssh.disconnect() } catch (_: Throwable) {}
            val kernelOk = cap4.contains("KERNEL_OK")
            if (!kernelOk || code4 != 0) {
                onLog("✗ 内核解压失败（exit=$code4，未捕获 KERNEL_OK）")
                onLog("----- 原始输出 -----")
                cap4.lineSequence().take(40).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            val lsLine = cap4.lineSequence().firstOrNull { it.contains("clash_meta") && it.contains("-rwx") }
            onLog("✓ 内核解压完成（${lsLine ?: "clash_meta 已就位"}，已校验 KERNEL_OK）")

            onLog("==================== 安装成功 ====================")
            onLog("路由器管理地址：http://${f.ip}/")
            onLog("请登录 OpenWrt → 服务 → OpenClash 进行配置")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 已强制结束安装")
            Result.success(false)
        } catch (e: Throwable) {
            onLog("✗ 安装异常：${e.message ?: e.javaClass.simpleName}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
