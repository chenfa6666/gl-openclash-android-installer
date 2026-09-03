package com.chenfa.openclashinstaller.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
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
 * 上：环境检查 + 下载按钮 + 当前连接摘要 + 操作按钮列表（连接测试 / 开始安装 / 安装风扇控制 / 解锁隐藏功能）
 * 点击操作按钮 → 弹出 OperationDialog 显示日志 + 强制结束按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    // 从本地导入 ipk / gz 到 app filesDir（用户：下载箭头 → 导入）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) vm.importLocalFile(uri)
    }

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
                    IconButton(
                        onClick = {
                            // 支持 .gz / .ipk / .tar.gz；系统选择器靠 MIME 兜底 */*
                            importLauncher.launch(arrayOf("*/*"))
                        },
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "本地导入 ipk / gz",
                        )
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
                Text(
                    "·下载走网络；右上 ↓ 按钮手动从手机导入 ipk / gz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
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
            Text(
                "gl 专属功能",
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            PrimaryButton(
                text = "安装风扇控制",
                onClick = vm::installFan,
                enabled = !state.busy,
                variant = PrimaryButtonVariant.TONAL,
                subtitle = "安装完成在GL 管理界面 系统-风扇控制",
            )
            PrimaryButton(
                text = "解锁隐藏功能",
                onClick = vm::unlockHidden,
                enabled = !state.busy,
                variant = PrimaryButtonVariant.TONAL,
                subtitle = "将 GL 管理界面中被 lang_hide 隐藏的菜单项改为中文可见",
            )

            Text(
                "修复 oppo reno5 pro usb 共享网络 bug",
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            PrimaryButton(
                text = "加载修复",
                onClick = vm::fixUsbTethering,
                enabled = !state.busy,
                variant = PrimaryButtonVariant.TONAL,
                subtitle = "内置修复版 RNDIS 内核模块，修复完成手动重启路由器生效",
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

