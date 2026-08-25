package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chenfa.openclashinstaller.ui.components.EnvCheckRow
import com.chenfa.openclashinstaller.ui.components.LabeledTextField
import com.chenfa.openclashinstaller.ui.components.PasswordField
import com.chenfa.openclashinstaller.ui.theme.LogBg

/**
 * 主屏幕 - 上下布局。
 *
 * 上 1/3：操作面板（环境检查 + 下载 + 输入字段 + 后续阶段按钮），可滚动。
 * 下 2/3：运行日志（浅米色背景，阶段 C+ 接入 ProgressLog + LogLine）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // 首次进入：加载持久化设置 + 环境检查
    LaunchedEffect(Unit) {
        vm.init0()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClash 安装器") },
                actions = {
                    IconButton(onClick = { vm.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { /* 阶段 F：导出完整日志 */ }) {
                        Icon(Icons.Default.Download, contentDescription = "导出日志")
                    }
                    IconButton(onClick = { vm.openAbout() }) {
                        Icon(Icons.Default.Info, contentDescription = "关于")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 上：操作面板
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "环境检查",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                state.envStatus.forEach { EnvCheckRow(it) }

                Spacer(Modifier.height(8.dp))

                Text(
                    "下载",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    "（阶段 C 接入：下载内核 / 下载 openclash）",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "路由器连接",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LabeledTextField(
                    label = "IP 地址",
                    value = state.fields.ip,
                    onValueChange = vm::setIp,
                )
                LabeledTextField(
                    label = "用户名",
                    value = state.fields.user,
                    onValueChange = vm::setUser,
                )
                PasswordField(
                    label = "密码",
                    value = state.fields.password,
                    onValueChange = vm::setPassword,
                )
                LabeledTextField(
                    label = "端口",
                    value = state.fields.port,
                    onValueChange = vm::setPort,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
                // 阶段 C 起加入连接测试 / 开始安装 / 强制结束 / 风扇控制按钮
            }

            // 下：运行日志（浅米色背景，阶段 C+ 接入 ProgressLog + LogLine）
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .background(LogBg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "（阶段 C+ 接入：运行日志列表）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
