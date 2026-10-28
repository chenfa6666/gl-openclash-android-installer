package com.chenfa.openclashinstaller.data

import com.chenfa.openclashinstaller.core.ext.containsCi

/**
 * 日志过滤：把 SSH 终端残片整理成适合阅读的日志行。
 *
 * 界面日志显示规则：
 *   1. 去掉 ANSI / CSI 控制序列与 `\r`
 *   2. 剥离行首被 chunk 切碎的终端控制残片（如 [?1004l [2J [H）
 *   3. 过滤噪音：命令回显、密码提示、openssh 路径、纯 CSI 残留
 *   4. 过滤内部校验标记：===VERIFY=== / KERNEL_OK / CONN_OK / HOST= / UNAME=
 *
 * 完整缓冲仍保留原始内容，导出时可排查全部细节。
 */
object LogFilter {

    /** 主入口：返回过滤后可显示的行；null 表示噪音不显示。 */
    fun processLine(raw: String): String? {
        val cleaned = sanitize(raw)
        if (cleaned.isBlank()) return null
        if (isNoiseLine(cleaned)) return null
        if (isInternalMarker(cleaned)) return null
        return cleaned
    }

    /** ANSI 转义序列（含颜色、光标移动）。 */
    private val ANSI_ESCAPE = Regex("""\u001B\[[0-?]*[ -/]*[@-~]""")

    /** 行首 CSI 残片：`[?1004l`、`[2J`、`[H`，可连续出现。 */
    private val LEADING_CSI = Regex("""^(?:\[[?;0-9]*[A-Za-z])+\s*""")

    /** 纯 CSI 残留（整行都是控制序列）。 */
    private val PURE_CSI = Regex("""^[\[\]?;0-9A-Za-z\s]+$""")

    private fun sanitize(raw: String): String {
        var line = raw
            .replace(ANSI_ESCAPE, "")
            .replace('\r', '\n')
            .replace('\u0000', ' ')
        // 一行内出现 \r 时只保留最后一段（进度条刷新的常见形态）
        if (line.contains('\n')) line = line.substringAfterLast('\n')
        line = line.replace(LEADING_CSI, "")
        return line.trimEnd()
    }

    private fun isNoiseLine(line: String): Boolean = when {
        line.length >= 2 && line[0] == '$' && line[1] == ' ' -> true
        line.containsCi("password:") -> true
        line.containsCi("openssh") -> true
        line.length <= 12 && PURE_CSI.matches(line) && line.any { it == '[' } -> true
        else -> false
    }

    private fun isInternalMarker(line: String): Boolean {
        val text = line.trim()
        return text == "===VERIFY===" ||
            text == "KERNEL_OK" ||
            text == "CONN_OK" ||
            text.startsWith("HOST=") ||
            text.startsWith("UNAME=")
    }
}
