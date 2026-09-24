package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 关于对话框：作者 chenfa + 版本号（液态玻璃版）。
 * 等价 Windows 版「帮助→关于」。
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val tokens = LocalGlassTokens.current
    GlassDialog(
        onDismissRequest = onDismiss,
        title = "关于",
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        },
    ) {
        Column {
            Text(
                "OpenClash 安装器",
                style = MaterialTheme.typography.titleLarge,
                color = tokens.onGlass,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.onGlass,
            )
            Text("作者: chenfa6666", color = tokens.onGlass)
            Text(
                "github: https://github.com/chenfa6666/gl-openclash-android-installer",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onGlassEmphasis,
            )
        }
    }
}
