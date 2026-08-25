package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.ensureActiveOrCancel
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.repo.SshClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 解锁 GL.iNet 管理界面的隐藏菜单项（用户给定命令）。
 *
 * 思路：GL.iNet 自带菜单配置放在 /usr/share/oui/menu.d/*.json 里；
 * 部分菜单项用 `"lang_hide": {"zh-cn": true, ...}` 方式在简体中文下隐藏。
 * 把紧随其后的 `"zh-cn"` 替换成 `"zh-tw"`，让简体中文版也能看到这些菜单项。
 *
 * 命令原文（用户指定）：
 *   find /usr/share/oui/menu.d -type f -name "*.json" \
 *       -exec sed -i '/"lang_hide"/{n;s/"zh-cn"/"zh-tw"/;}' {} +
 *
 * 校验：sed 替换后，再 grep 一遍被隐藏的 JSON。
 */
object WorkerUnlockHidden {

    private const val CMD = """find /usr/share/oui/menu.d -type f -name "*.json" -exec sed -i '/"lang_hide"/{n;s/"zh-cn"/"zh-tw"/;}' {} +"""

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
                onLog("⚠ SSH 重连失败，跳过校验（结果可能已生效但无法确认）")
                // sed 已成功，即使校验不到也当作成功
                return@worker Result.success(true)
            }
            // 校验：如果替换成功，menu.d 下 "lang_hide" 后紧跟 "zh-cn" 的情况应该只剩很少
            // 这里只要 grep 能搜到 "zh-tw" 在 lang_hide 之后，就认为生效
            val verify = """grep -n -A1 '"lang_hide"' /usr/share/oui/menu.d/*.json 2>/dev/null | head -n 80 || echo "NO_FILES_OR_NO_MATCH""""
            val (c2, out2) = ssh.execCommand(verify, timeoutMs = 20_000L).getOrElse { (-1 to it.message.orEmpty()) }
            runCatching { ssh.disconnect() }
            if (c2 != 0) {
                onLog("⚠ 校验命令执行失败（exit=$c2），直接假定已替换完成")
            }
            val replacedTw = out2.lineSequence().filter { it.contains("zh-tw", ignoreCase = false) }.count()
            val remainingCn = out2.lineSequence().filter { it.contains("zh-cn", ignoreCase = false) }.count()
            onLog("· 校验结果：lang_hide 下已转 zh-tw 的行数=$replacedTw，仍保留 zh-cn 的行数=$remainingCn")
            out2.lineSequence().take(60).forEach { onLog("  $it") }
            if (replacedTw == 0 && remainingCn == 0) {
                onLog("⚠ 没有检测到 lang_hide 匹配项（可能 menu.d 路径不同，或当前固件没有被隐藏的中文菜单）")
                onLog("提示：如果管理界面仍看不到隐藏项，请登录 SSH 手动执行原命令确认。")
            }

            onLog("==================== 操作完成 ====================")
            onLog("建议：退出 GL.iNet 管理后台重新登录（或清空浏览器缓存），隐藏菜单项即可显示。")
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
