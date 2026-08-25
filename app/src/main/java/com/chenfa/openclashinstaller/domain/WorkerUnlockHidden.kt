package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 解锁 GL.iNet 管理界面隐藏菜单项（用户指定命令）。
 *
 * 思路：GL.iNet 菜单 JSON 位于 /usr/share/oui/menu.d/，
 * 部分菜单项用 "lang_hide" 字段在简体中文下隐藏。sed 把紧随其后的
 * "zh-cn" 替换成 "zh-tw"，简体中文管理端即可看到。
 *
 * 用户给定原命令：
 *   find /usr/share/oui/menu.d -type f -name "*.json" \
 *       -exec sed -i '/"lang_hide"/{n;s/"zh-cn"/"zh-tw"/;}' {} +
 */
object WorkerUnlockHidden {

    // 注意：用普通字符串（不用 triple-quote raw），避免 lexer 被 shell 引号 /
    // 重定向符号 2>/dev/null 的 /* 触发误判为 block comment open。
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
            val port = fields.port.toIntOrNull() ?: 22
            ensureActiveOrCancel()

            onLog("==================== 步骤 1/2：执行 sed 批量替换 ====================")
            val conn1 = ssh.connect(fields.user, fields.ip, port, fields.password)
            if (conn1.isFailure) {
                onLog("✗ SSH 连接失败：${conn1.exceptionOrNull()?.message ?: conn1.exceptionOrNull()?.javaClass?.simpleName}")
                return@worker Result.success(false)
            }
            onLog("· $CMD")
            val (c1, out1) = ssh.execCommand(CMD, timeoutMs = 30_000L).getOrElse { (-1 to it.message.orEmpty()) }
            runCatching { ssh.disconnect() }
            if (c1 != 0) {
                onLog("✗ sed/find 命令执行失败（exit=$c1）")
                out1.lineSequence().take(40).forEach { onLog("  $it") }
                return@worker Result.success(false)
            }
            onLog("✓ sed 替换完成")

            onLog("==================== 步骤 2/2：校验结果 ====================")
            val conn2 = ssh.connect(fields.user, fields.ip, port, fields.password)
            if (conn2.isFailure) {
                onLog("⚠ SSH 重连失败，跳过校验（sed 可能已生效）")
                return@worker Result.success(true)
            }
            val (c2, out2) = ssh.execCommand(VERIFY, timeoutMs = 20_000L).getOrElse { (-1 to it.message.orEmpty()) }
            runCatching { ssh.disconnect() }
            if (c2 != 0) {
                onLog("⚠ 校验命令执行失败（exit=$c2），直接假定已替换完成")
            }
            val replacedTw = out2.lineSequence().filter { it.contains("zh-tw", ignoreCase = false) }.count()
            val remainingCn = out2.lineSequence().filter { it.contains("zh-cn", ignoreCase = false) }.count()
            onLog("· 校验结果：lang_hide 下 zh-tw 行数=$replacedTw，仍保留 zh-cn 行数=$remainingCn")
            out2.lineSequence().take(60).forEach { onLog("  $it") }
            if (replacedTw == 0 && remainingCn == 0) {
                onLog("⚠ 未检测到 lang_hide 匹配项（menu.d 路径不同或当前固件无被隐藏菜单）")
                onLog("提示：如管理界面仍看不到，请 SSH 登录后手动执行原命令确认。")
            }

            onLog("==================== 操作完成 ====================")
            onLog("建议：退出 GL.iNet 管理后台重新登录（或清浏览器缓存），隐藏菜单项即可显示。")
            Result.success(true)
        } catch (ce: CancellationException) {
            onLog("! 已强制结束解锁")
            Result.success(false)
        } catch (e: Throwable) {
            onLog("✗ 解锁异常：${e.message ?: e.javaClass.simpleName}")
            Result.failure(e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }
}
