package com.chenfa.openclashinstaller.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.ui.theme.LogBg

/**
 * 操作弹窗（像设置对话框一样，但全屏）：
 *  - 顶部 TopAppBar：标题 + 关闭按钮（busy 时禁用）
 *  - 中部：日志列表（ProgressLog，进度原地刷新，自动滚到底）
 *  - 底部：强制结束按钮（busy 时显示红色，否则显示"关闭"按钮）
 *
 * 等价 Windows 版独立操作面板，把"日志 + 强制结束"合并到一个独立界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationDialog(
    title: String,
    entries: List<LogEntry>,
    busy: Boolean,
    onClose: () -> Unit,
    onAbort: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BackHandler(enabled = !busy) { onClose() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose, enabled = !busy) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 日志区（占大部分）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(LogBg),
                ) {
                    if (entries.isEmpty()) {
                        Text(
                            "（暂无日志）",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        ProgressLog(entries = entries)
                    }
                }

                // 底部：强制结束或关闭
                if (busy) {
                    PrimaryButton(
                        text = "强制结束安装",
                        onClick = onAbort,
                        variant = PrimaryButtonVariant.ERROR,
                    )
                } else {
                    PrimaryButton(
                        text = "关闭",
                        onClick = onClose,
                    )
                }
            }
        }
    }
}
