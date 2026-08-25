package com.chenfa.openclashinstaller.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 强制结束确认对话框。
 * 等价 Windows 版 IDC_BTN_ABORT 的 MB_YESNO 默认否（安全）。
 */
@Composable
fun ConfirmAbortDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("强制结束安装") },
        text = { Text("确认强制结束当前操作？将中断下载/SSH 连接。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("是，强制结束") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("否") }
        },
    )
}
