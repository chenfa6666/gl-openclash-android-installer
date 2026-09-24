package com.chenfa.openclashinstaller.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chenfa.openclashinstaller.data.model.UiEvent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.chenfa.openclashinstaller.ui.components.ConfirmAbortDialog
import com.chenfa.openclashinstaller.ui.components.GlassSurface
import com.chenfa.openclashinstaller.ui.components.LiquidBottomTab
import com.chenfa.openclashinstaller.ui.components.LiquidBottomTabs
import com.chenfa.openclashinstaller.ui.components.LiquidTab
import com.chenfa.openclashinstaller.ui.components.OperationDialog
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LiquidGlassRoot
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens
import com.chenfa.openclashinstaller.ui.theme.glass

/**
 * 主屏幕：三个页面（安装 / 设置 / 关于）通过悬浮液态底栏切换。
 *
 * 操作流程弹窗（OperationDialog）与强制结束确认（ConfirmAbortDialog）保持不变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModelFactory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val tokens = LocalGlassTokens.current
    var selectedTab by rememberSaveable { mutableStateOf(LiquidTab.INSTALL) }

    // 从本地导入 ipk / gz 到 app filesDir（安装页顶栏：下载箭头 → 导入）
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

    // 玻璃根：液态壁纸采样层 + 前景 UI
    LiquidGlassRoot {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                LiquidTab.INSTALL -> "OpenClash 安装器"
                                LiquidTab.SETTINGS -> "设置"
                                LiquidTab.ABOUT -> "关于"
                            },
                        )
                    },
                    actions = {
                        // 本地导入只属于安装流程
                        if (selectedTab == LiquidTab.INSTALL) {
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
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = tokens.onGlass,
                        navigationIconContentColor = tokens.onGlass,
                        actionIconContentColor = tokens.onGlass,
                    ),
                    modifier = Modifier.glass(
                        shape = GlassShapes.bar,
                        tint = tokens.bar,
                        blurRadius = 20.dp,
                        shadowRadius = 12.dp,
                    ),
                )
            },
            snackbarHost = {
                // 避让悬浮底栏，显示在其上方
                SnackbarHost(
                    hostState = snackbarHost,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 64.dp),
                ) { data ->
                    GlassSurface(
                        shape = GlassShapes.snack,
                        tint = tokens.snack,
                        blurRadius = 20.dp,
                        shadowRadius = 12.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.onGlass,
                        )
                    }
                }
            },
            ) { padding ->
                // 底栏改为悬浮覆盖层，不再占 Scaffold 布局；内容底部避让底栏高度
                Crossfade(
                    targetState = selectedTab,
                    label = "pageSwitch",
                    modifier = Modifier.padding(bottom = 64.dp),
                ) { tab ->
                    when (tab) {
                        LiquidTab.INSTALL -> InstallPage(
                            vm = vm,
                            contentPadding = padding,
                            onEditConnection = { selectedTab = LiquidTab.SETTINGS },
                        )

                        LiquidTab.SETTINGS -> SettingsPage(
                            vm = vm,
                            contentPadding = padding,
                            onSaved = { selectedTab = LiquidTab.INSTALL },
                        )

                        LiquidTab.ABOUT -> AboutPage(contentPadding = padding)
                    }
                }
            }

            // 悬浮液态玻璃底栏：覆盖在页面内容之上，避让系统手势导航条，居中收窄（宽度 3/4）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab.ordinal },
                    onTabSelected = { selectedTab = LiquidTab.entries[it] },
                    tabsCount = LiquidTab.entries.size,
                    modifier = Modifier.fillMaxWidth(0.75f),
                ) {
                    LiquidTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val color = if (isSelected) tokens.primary else tokens.onGlassEmphasis
                        LiquidBottomTab(onClick = { selectedTab = tab }) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = color,
                                modifier = Modifier.height(20.dp),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                            )
                        }
                    }
                }
            }
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
