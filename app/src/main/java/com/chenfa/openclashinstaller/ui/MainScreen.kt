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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chenfa.openclashinstaller.data.model.UiEvent
import com.chenfa.openclashinstaller.ui.components.AboutDialog
import com.chenfa.openclashinstaller.ui.components.CollapsibleCard
import com.chenfa.openclashinstaller.ui.components.ConfirmAbortDialog
import com.chenfa.openclashinstaller.ui.components.EnvCheckRow
import com.chenfa.openclashinstaller.ui.components.PrimaryButton
import com.chenfa.openclashinstaller.ui.components.PrimaryButtonVariant
import com.chenfa.openclashinstaller.ui.components.SettingsDialog
import com.chenfa.openclashinstaller.ui.theme.LogBg

/**
 * 主屏幕 - 上下布局 2:1。
 *
 * 上 2/3：操作面板（可滚动）
 *   - 环境检查（折叠 Card，默认展开）
 *   - 下载（折叠 Card，默认展开；阶段 C 接入按钮）
 *   - 当前连接摘要 + 编辑按钮（点击打开设置对话框）
 *   - 阶段 C 起的操作按钮（连接测试/开始安装/强制结束/风扇控制）
 *
 * 下 1/3：运行日志（可滚动，阶段 C+ 接入 ProgressLog）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    // 首次进入：加载持久化设置 + 环境检查
    LaunchedEffect(Unit) {
        vm.init0()
    }

    // 收集一次性事件（Toast）
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is UiEvent.Toast -> snackbarHost.showSnackbar(ev.msg)
                else -> {}
            }
        }
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
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
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
                    .weight(2f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 环境检查 - 可折叠
                CollapsibleCard(title = "环境检查", defaultExpanded = true) {
                    state.envStatus.forEach { EnvCheckRow(it) }
                }

                // 下载 - 可折叠
                CollapsibleCard(title = "下载", defaultExpanded = true) {
                    Text(
                        "（阶段 C 接入：下载内核 / 下载 openclash）",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // 当前连接 - 紧凑摘要 + 编辑按钮
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "当前连接",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "${state.fields.user}@${state.fields.ip}:${state.fields.port}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        IconButton(onClick = { vm.openSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "编辑连接")
                        }
                    }
                }

                // 阶段 C 起加入：连接测试 / 开始安装 / 强制结束 / 风扇控制按钮
                // 当前阶段 B 仅占位
                Text(
                    "（阶段 C 起加入操作按钮）",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp),
                )
            }

            // 下：运行日志（可滚动，浅米色背景，阶段 C+ 接入 ProgressLog）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(LogBg)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    "（阶段 C+ 接入：运行日志列表）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    // 对话框们
    if (state.settingsOpen) {
        SettingsDialog(
            initialKernelUrl = state.kernelUrl,
            initialOpenclashUrl = state.openclashUrl,
            initialFanUrl = state.fanUrl,
            initialFields = state.fields,
            onDismiss = vm::closeSettings,
            onSave = vm::saveSettings,
        )
    }
    if (state.aboutOpen) {
        AboutDialog(onDismiss = vm::closeAbout)
    }
    if (state.confirmAbortOpen) {
        ConfirmAbortDialog(
            onConfirm = {
                vm.closeConfirmAbort()
                // 阶段 C+：vm.abort()
            },
            onDismiss = vm::closeConfirmAbort,
        )
    }
}
