package com.chenfa.openclashinstaller.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 强制结束确认对话框（液态玻璃版）。
 * 等价 Windows 版 IDC_BTN_ABORT 的 MB_YESNO 默认否（安全）。
 */
@Composable
fun ConfirmAbortDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    GlassDialog(
        onDismissRequest = onDismiss,
        title = "强制结束安装",
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("是，强制结束") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("否") }
        },
    ) {
        Text(
            "确认强制结束当前操作？将中断下载/SSH 连接。",
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.onGlass,
        )
    }
}
