package com.chenfa.openclashinstaller.data

import com.chenfa.openclashinstaller.core.ext.containsCi

/**
 * 日志过滤：等价 Windows 版 IsNoiseLine + StripLeadingCtrl。
 *
 * 界面日志显示规则：
 *   1. 剥离行首终端控制残留（跨 chunk 切碎的 ESC 序列残片，如 [?1004l [2J [H）
 *   2. 过滤噪音行：命令回显 / 密码提示 / openssh 路径 / 纯 CSI 残留
 *
 * 完整缓冲 g_fullLog 仍保留所有原始字节，导出能查到。
 *
 * 阶段 F 完整接线；阶段 B 仅占位（未被 ViewModel 调用）。
 */
object LogFilter {

    /** 主入口：返回过滤后可显示的行；null 表示噪音不显示。 */
    fun processLine(raw: String): String? {
        val s = stripLeadingCtrl(raw)
        if (s.isEmpty() || isNoiseLine(s)) return null
        return s
    }

    /** 等价 C++ IsNoiseLine：命令回显 / 密码提示 / openssh 路径 / 纯 CSI 残留。 */
    private fun isNoiseLine(line: String): Boolean = when {
        line.length >= 2 && line[0] == '$' && line[1] == ' ' -> true
        line.containsCi("password:") -> true
        line.containsCi("openssh") -> true
        line.length <= 12 && line[0] == '[' && line.all(::isCtrlChar) -> true
        else -> false
    }

    /** 等价 C++ StripLeadingCtrl：剥离行首连续 CSI 残片 [ + 可选? + 数字/;* + 字母。 */
    private fun stripLeadingCtrl(line: String): String {
        var i = 0
        while (i + 1 < line.length && line[i] == '[') {
            var j = i + 1
            if (j < line.length && line[j] == '?') ++j
            while (j < line.length && (line[j].isDigit() || line[j] == ';')) ++j
            if (j < line.length && (line[j] in 'A'..'Z' || line[j] in 'a'..'z')) {
                i = j + 1
                continue
            }
            break
        }
        return if (i == 0) line else line.substring(i)
    }

    private fun isCtrlChar(c: Char): Boolean =
        c.isDigit() || c == '?' || c == ';' || c == '[' ||
            c in 'A'..'Z' || c in 'a'..'z'
}
