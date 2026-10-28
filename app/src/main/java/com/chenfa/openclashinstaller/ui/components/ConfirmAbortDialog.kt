package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 强制结束确认。
 *
 * 默认动作是「取消」，避免误触中断正在写入的安装流程；
 * 强制结束只中断当前任务，不回滚已写入路由器 /tmp 的临时文件。
 */
@Composable
fun ConfirmAbortDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = "强制结束任务？",
        summary = "将立即中断当前的下载或 SSH 连接。已经上传到路由器的临时文件不会自动清理。",
        onDismissRequest = onDismiss,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    minWidth = 72.dp,
                    minHeight = 40.dp,
                    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(),
                )
                Button(
                    onClick = onConfirm,
                    minWidth = 96.dp,
                    minHeight = 40.dp,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError,
                    ),
                ) {
                    Text("强制结束")
                }
            }
        },
    )
}
