package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 解锁 GL.iNet 管理界面隐藏菜单项。
 *
 * GL.iNet 菜单 JSON 位于 /usr/share/oui/menu.d/，部分菜单项用 "lang_hide"
 * 字段在简体中文下隐藏。把紧随其后的 "zh-cn" 替换成 "zh-tw" 后，
 * 简体中文管理端即可看到这些菜单。
 */
object WorkerUnlockHidden {

    private const val CMD =
        "find /usr/share/oui/menu.d -type f -name \"*.json\" -exec sed -i " +
            "'/\"lang_hide\"/{n;s/\"zh-cn\"/\"zh-tw\"/;}' {} +"

    private const val VERIFY =
        "grep -n -A1 '\"lang_hide\"' /usr/share/oui/menu.d/*.json " +
            "2>/dev/null | head -n 80 || echo NO_FILES_OR_NO_MATCH"

    suspend fun execute(
        ssh: SshClient,
        fields: ConnFields,
        onLog: suspend (String) -> Unit,
    ): Result<Boolean> = withContext(Dispatchers.IO) worker@{
        try {
            val f = fields.normalized()
            val port = f.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            onLog("步骤 1/2 · 修改菜单配置")
            onLog("· 查找 lang_hide 菜单并将 zh-cn 替换为 zh-tw")
            val conn1 = ssh.connect(f.user, f.ip, port, f.password)
            if (conn1.isFailure) {
                onLog("✗ SSH 连接失败：${conn1.exceptionOrNull()?.toUserMessage("请检查 IP、端口和 root 密码")}")
                return@worker Result.success(false)
            }
            val (c1, out1) = ssh.execCommand(CMD, timeoutMs = 30_000L).getOrElse { e ->
                onLog("✗ 菜单修改命令执行失败：${e.toUserMessage("请稍后重试")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(false)
            }
            runCatching { ssh.disconnect() }
            if (c1 != 0) {
                onLog("✗ 菜单修改失败（exit=$c1）")
                onLog("--- 命令输出（前 30 行）---")
                out1.lineSequence().take(30).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            onLog("✓ 菜单配置已更新")

            onLog("步骤 2/2 · 校验修改结果")
            val conn2 = ssh.connect(f.user, f.ip, port, f.password)
            if (conn2.isFailure) {
                onLog("⚠ SSH 重连失败，跳过校验（修改可能已经生效）")
                return@worker Result.success(true)
            }
            val (c2, out2) = ssh.execCommand(VERIFY, timeoutMs = 20_000L).getOrElse { e ->
                onLog("⚠ 校验命令执行失败：${e.toUserMessage("请手动确认菜单是否出现")}")
                runCatching { ssh.disconnect() }
                return@worker Result.success(true)
            }
            runCatching { ssh.disconnect() }
            if (c2 != 0) {
                onLog("⚠ 校验命令返回 exit=$c2，修改可能已生效")
            }
            val replacedTw = out2.lineSequence().count { it.contains("zh-tw") }
            val remainingCn = out2.lineSequence().count { it.contains("zh-cn") }
            onLog("· 校验结果：已替换 $replacedTw 行，仍保留 zh-cn $remainingCn 行")
            if (replacedTw == 0 && remainingCn == 0) {
                onLog("⚠ 未找到 lang_hide 匹配项，当前固件可能没有隐藏菜单")
                onLog("· 如仍看不到菜单，请登录路由器手动确认 menu.d 路径")
            }

            onLog("✓ 解锁完成")
            onLog("· 请退出 GL 管理后台重新登录（或清除浏览器缓存）后查看")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 解锁已取消")
            throw ce
        } catch (e: Throwable) {
            onLog("✗ 解锁异常：${e.toUserMessage("请稍后重试")}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
