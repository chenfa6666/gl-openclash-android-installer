package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenfa.openclashinstaller.data.model.UiState
import com.chenfa.openclashinstaller.ui.components.BlurredBar
import com.chenfa.openclashinstaller.ui.components.rememberBlurBackdrop
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import com.chenfa.openclashinstaller.ui.theme.LocalEnableBlur
import com.chenfa.openclashinstaller.ui.theme.appIsDarkTheme
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「环境」页：
 *  - 顶部连接卡：未填写 -> 去设置；检测中 -> 进度；成功 -> 设备名；失败 -> 原因
 *  - 环境检查与下载整合为一张卡，点击条目直接下载/重新下载/安装
 */
@Composable
fun EnvironmentPage(
    vm: MainViewModel,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    LaunchedEffect(state.fields) {
        if (state.fields.isComplete() &&
            !state.connChecked &&
            !state.connChecking &&
            !state.busy
        ) {
            vm.checkConnection()
        }
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "环境",
                    actions = {
                        IconButton(onClick = vm::refreshEnv) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = "刷新环境检查",
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = if (backdrop != null) {
                Modifier.layerBackdrop(backdrop)
            } else {
                Modifier
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ConnectionHero(
                            state = state,
                            onOpenSettings = onOpenSettings,
                            onCheck = vm::checkConnection,
                        )

                        EnvironmentChecksCard(
                            state = state,
                            onRefresh = vm::refreshEnv,
                            onCheckConnection = vm::checkConnection,
                            onDownloadKernel = vm::downloadKernel,
                            onDownloadOpenclash = vm::downloadOpenclash,
                            onDownloadFan = vm::downloadFan,
                        )

                        Spacer(Modifier.height(bottomPadding + 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionHero(
    state: UiState,
    onOpenSettings: () -> Unit,
    onCheck: () -> Unit,
) {
    val dark = appIsDarkTheme
    val configured = state.fields.isComplete()
    val checking = state.connChecking
    val connected = state.connChecked && state.connDeviceName != null
    val failed = state.connChecked && !connected && !checking

    val container = when {
        isDynamicColor -> colorScheme.secondaryContainer
        checking -> if (dark) Color(0xFF102A44) else Color(0xFFE8F1FF)
        connected -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        failed -> colorScheme.errorContainer
        else -> AppStatus.warningContainer
    }
    val content = when {
        isDynamicColor -> colorScheme.onSecondaryContainer
        checking -> if (dark) Color(0xFF9CC7FF) else Color(0xFF0A57C2)
        connected -> if (dark) Color(0xFFDFFAE4) else Color(0xFF164A29)
        failed -> colorScheme.onErrorContainer
        else -> AppStatus.onWarningContainer
    }
    val accent = when {
        isDynamicColor -> colorScheme.primary
        checking -> colorScheme.primary
        connected -> AppStatus.success
        failed -> colorScheme.error
        else -> AppStatus.warning
    }
    val title = when {
        !configured -> "未填写连接参数"
        checking -> "正在连接…"
        connected -> state.connDeviceName.orEmpty().ifBlank { "已连接" }
        failed -> "连接失败"
        else -> "待检测连接"
    }
    val detail = when {
        !configured -> state.fields.firstError() ?: "请先填写 IP、用户名、密码和端口"
        checking -> "正在验证 SSH 连接与设备信息"
        connected -> "SSH 连接成功 · ${state.fields.user.trim()}@${state.fields.ip.trim()}:${state.fields.port.trim()}"
        failed -> state.connError ?: "连接失败，请检查网络或 SSH 配置"
        else -> "点击检测连接"
    }

    Card(
        colors = CardDefaults.defaultColors(color = container, contentColor = content),
        onClick = {
            when {
                !configured -> onOpenSettings()
                checking -> Unit
                else -> onCheck()
            }
        },
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = !checking,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 26.dp, y = 26.dp)
                    .size(120.dp)
                    .border(8.dp, accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(62.dp),
                        strokeWidth = 6.dp,
                        colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = accent,
                            backgroundColor = accent.copy(alpha = 0.15f),
                        ),
                    )
                } else {
                    Icon(
                        imageVector = when {
                            connected -> MiuixIcons.Ok
                            failed || !configured -> MiuixIcons.Report
                            else -> MiuixIcons.Settings
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp, 14.dp)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (connected) "设备已连接" else "SSH 连接检测",
                    fontSize = 15.sp,
                    color = content.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 1.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = detail,
                    fontSize = 15.sp,
                    color = content.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 76.dp),
                )
            }
        }
    }
}

@Composable
private fun EnvironmentChecksCard(
    state: UiState,
    onRefresh: () -> Unit,
    onCheckConnection: () -> Unit,
    onDownloadKernel: () -> Unit,
    onDownloadOpenclash: () -> Unit,
    onDownloadFan: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        state.envStatus.forEach { item ->
            val isFan = item.name.startsWith("风扇")
            val isKernel = item.name.startsWith("Clash")
            val isIpk = item.name.startsWith("OpenClash")
            val isSsh = item.name.startsWith("SSH")
            val action = when {
                isKernel -> onDownloadKernel
                isIpk -> onDownloadOpenclash
                isFan -> onDownloadFan
                else -> onCheckConnection
            }
            val hint = when {
                isSsh -> if (item.ok) "就绪" else "异常"
                item.ok -> "点击重新下载"
                else -> "点击下载"
            }
            ArrowPreference(
                title = item.name,
                summary = listOfNotNull(item.detail, hint).joinToString(" · "),
                startAction = {
                    Icon(
                        imageVector = when {
                            isKernel -> MiuixIcons.Download
                            isIpk -> MiuixIcons.Store
                            isFan -> MiuixIcons.Tune
                            else -> MiuixIcons.Ok
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                        tint = if (item.ok) colorScheme.onBackground else colorScheme.error,
                    )
                },
                onClick = action,
                enabled = !state.busy,
            )
        }
        BasicComponent(
            title = "刷新检查",
            summary = "重新读取本地安装包与连接状态",
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onSurface,
                )
            },
            onClick = onRefresh,
        )
    }
}
