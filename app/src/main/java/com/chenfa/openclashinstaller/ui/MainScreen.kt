package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.chenfa.openclashinstaller.ui.components.DownloadButtonsRow
import com.chenfa.openclashinstaller.ui.components.EnvCheckRow
import com.chenfa.openclashinstaller.ui.components.OperationDialog
import com.chenfa.openclashinstaller.ui.components.PrimaryButton
import com.chenfa.openclashinstaller.ui.components.PrimaryButtonVariant
import com.chenfa.openclashinstaller.ui.components.SettingsDialog

/**
 * 主屏幕 - 纯操作面板（无日志区）。
 *
 * 上：环境检查 + 下载按钮 + 当前连接摘要 + 操作按钮列表（连接测试 / 开始安装 / 安装风扇控制）
 * 点击操作按钮 → 弹出 OperationDialog 显示日志 + 强制结束按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.init0() }

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
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 环境检查
            CollapsibleCard(title = "环境检查", defaultExpanded = true) {
                state.envStatus.forEach { EnvCheckRow(it) }
            }

            // 下载
            CollapsibleCard(title = "下载", defaultExpanded = true) {
                DownloadButtonsRow(
                    onKernel = vm::downloadKernel,
                    onOpenclash = vm::downloadOpenclash,
                    enabled = !state.busy,
                )
            }

            // 当前连接摘要 + 编辑按钮
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
                        Text("当前连接", style = MaterialTheme.typography.titleLarge)
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

            // 操作按钮列表
            PrimaryButton(
                text = "连接测试",
                onClick = vm::testConn,
                enabled = !state.busy,
            )
            PrimaryButton(
                text = "开始安装",
                onClick = vm::install,
                enabled = !state.busy,
            )
            androidx.compose.material3.Text(
                "gl 专属功能",
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            PrimaryButton(
                text = "安装风扇控制",
                onClick = vm::installFan,
                enabled = !state.busy,
                variant = PrimaryButtonVariant.TONAL_OUTLINE,
            )
        }
    }

    // 操作弹窗：日志 + 强制结束
    if (state.operationOpen) {
        OperationDialog(
            title = state.operationTitle,
            entries = state.logEntries,
            busy = state.busy,
            onClose = vm::closeOperation,
            onAbort = {
                vm.abort()
            },
        )
    }

    // 设置对话框
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
                vm.abort()
            },
            onDismiss = vm::closeConfirmAbort,
        )
    }
}
