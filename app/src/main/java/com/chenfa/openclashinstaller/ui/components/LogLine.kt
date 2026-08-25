package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.ui.theme.LogError
import com.chenfa.openclashinstaller.ui.theme.LogNormal
import com.chenfa.openclashinstaller.ui.theme.LogSuccess
import com.chenfa.openclashinstaller.ui.theme.LogWarn
import com.chenfa.openclashinstaller.ui.theme.LogNormalDark

/**
 * 单行日志渲染：按 kind 着色。
 * 等价 Windows 版按行首字符 ✓✗·! 着色。
 */
@Composable
fun LogLine(
    text: String,
    kind: LogKind,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when (kind) {
        LogKind.SUCCESS -> LogSuccess
        LogKind.ERROR -> LogError
        LogKind.WARN -> LogWarn
        LogKind.PROGRESS -> if (isDark) LogNormalDark else LogNormal
        LogKind.NORMAL -> if (isDark) LogNormalDark else LogNormal
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier.fillMaxWidth().padding(vertical = 1.dp),
    )
}
