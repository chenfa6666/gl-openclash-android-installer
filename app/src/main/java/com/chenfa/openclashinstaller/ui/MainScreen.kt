package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 主屏幕 - 阶段 A 占位实现。
 *
 * 阶段 B 起会接入 ViewModel 并替换为：
 *   Scaffold {
 *     Row {
 *       LeftPanel(...)   // weight 1f，环境检查 + 下载按钮 + 输入字段 + 开始/强制结束/风扇按钮
 *       ProgressLog(...) // weight 2f，日志列表
 *     }
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClash 安装器") },
                actions = {
                    IconButton(onClick = { /* 阶段 E：打开设置 */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { /* 阶段 F：导出完整日志 */ }) {
                        Icon(Icons.Default.Download, contentDescription = "导出日志")
                    }
                    IconButton(onClick = { /* 阶段 F：关于对话框 */ }) {
                        Icon(Icons.Default.Info, contentDescription = "关于")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("阶段 A 骨架：项目就绪，等待阶段 B 接入环境检查 + 设置")
        }
    }
}
