package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chenfa.openclashinstaller.BuildConfig

/**
 * 关于对话框：作者 chenfa + 版本号。
 * 等价 Windows 版「帮助→关于」。
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = {
            Column {
                Text("OpenClash 安装器", style = MaterialTheme.typography.titleLarge)
                Text("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Text("作者: chenfa")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        },
    )
}
