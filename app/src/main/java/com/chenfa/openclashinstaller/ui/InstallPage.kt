package com.chenfa.openclashinstaller.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.data.model.OpStatus
import com.chenfa.openclashinstaller.data.model.UiState
import com.chenfa.openclashinstaller.ui.components.BlurredBar
import com.chenfa.openclashinstaller.ui.components.rememberBlurBackdrop
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import com.chenfa.openclashinstaller.ui.theme.LocalEnableBlur
import com.chenfa.openclashinstaller.ui.theme.appIsDarkTheme
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「安装」页，按 KernelSU Home 的 Miuix 结构改写：
 * 顶部大标题 -> 绿色状态卡 -> 信息卡 -> 操作卡 -> 支持链接。
 */
@Composable
fun InstallPage(
    vm: MainViewModel,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenEnvironment: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) vm.importLocalFile(uri) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "OpenClash",
                    actions = {
                        IconButton(
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.busy && !state.importing,
                        ) {
                            if (state.importing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.Import,
                                    contentDescription = "导入本地安装包",
                                    tint = if (state.busy) {
                                        colorScheme.disabledOnSecondaryVariant
                                    } else {
                                        colorScheme.onBackground
                                    },
                                )
                            }
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
                    if (state.outcome != null) {
                        OutcomeRow(
                            state = state,
                            onViewLog = vm::reopenOperation,
                        )
                    }

                    ActionsCard(
                        state = state,
                        onInstall = vm::install,
                        onTest = vm::testConn,
                        onFan = vm::installFan,
                        onUnlock = vm::unlockHidden,
                        onUsb = vm::fixUsbTethering,
                    )

                    Spacer(Modifier.height(bottomPadding + 12.dp))
                }
            }
        }
    }
}
}

@Composable
private fun StatusCard(
    state: UiState,
    onEditConnection: () -> Unit,
    onInstall: () -> Unit,
) {
    val dark = appIsDarkTheme
    val ready = state.fields.isComplete()
    val busy = state.busy
    val outcome = state.outcome
    val success = outcome?.status == OpStatus.SUCCESS
    val failed = outcome?.status == OpStatus.FAILED

    val container = when {
        isDynamicColor -> colorScheme.secondaryContainer
        busy -> if (dark) Color(0xFF102A44) else Color(0xFFE8F1FF)
        success -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        failed -> if (dark) Color(0xFF3A1715) else Color(0xFFFFE7E4)
        !ready -> if (dark) Color(0xFF3A2A0A) else Color(0xFFFFF1D6)
        else -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
    }
    val content = when {
        isDynamicColor -> colorScheme.onSecondaryContainer
        busy -> if (dark) Color(0xFF9CC7FF) else Color(0xFF0A57C2)
        success || (ready && !failed) -> if (dark) Color(0xFFDFFAE4) else Color(0xFF164A29)
        failed -> if (dark) Color(0xFFFFDAD6) else Color(0xFF8A0000)
        else -> if (dark) Color(0xFFFFE2A8) else Color(0xFF4A2C00)
    }
    val accent = when {
        isDynamicColor -> colorScheme.primary
        busy -> colorScheme.primary
        failed -> colorScheme.error
        !ready -> Color(0xFFFF9F0A)
        else -> Color(0xFF36D167)
    }
    val title = when {
        busy -> "正在执行"
        failed -> "需要处理"
        success -> "完成"
        !ready -> "待完善"
        else -> "准备就绪"
    }
    val detail = when {
        busy -> state.operationTitle.ifBlank { "任务进行中" }
        outcome != null -> outcome.summary
        !ready -> state.fields.firstError() ?: "请先完善连接信息"
        else -> "${state.fields.user.trim()}@${state.fields.ip.trim()}:${state.fields.port.trim()}"
    }
    val subtitle = "版本 ${BuildConfig.VERSION_NAME}"

    Card(
        colors = CardDefaults.defaultColors(color = container, contentColor = content),
        onClick = {
            when {
                busy -> Unit
                !ready -> onEditConnection()
                else -> onInstall()
            }
        },
        showIndication = !busy,
        pressFeedbackType = PressFeedbackType.Tilt,
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
                if (busy) {
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
                            failed -> MiuixIcons.Close2
                            !ready -> MiuixIcons.Report
                            else -> MiuixIcons.Ok
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
                )
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = content.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 1.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = detail,
                    fontSize = 15.sp,
                    color = content.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 76.dp),
                )
                if (!busy) {
                    Text(
                        text = if (ready) "点击开始安装" else "点击完善连接信息",
                        fontSize = 13.sp,
                        color = content.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(state: UiState) {
    val files = state.downloadedFiles
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoText(
                icon = MiuixIcons.Settings,
                title = "路由器连接",
                content = if (state.fields.isComplete()) {
                    "${state.fields.user.trim()}@${state.fields.ip.trim()}:${state.fields.port.trim()}"
                } else {
                    state.fields.firstError() ?: "未配置"
                },
            )
            InfoText(
                icon = MiuixIcons.Ok,
                title = "环境检查",
                content = "${state.envStatus.count { it.ok }} / ${state.envStatus.size} 项就绪",
            )
            InfoText(
                icon = MiuixIcons.Download,
                title = "Clash 内核",
                content = if (files.kernelReady) {
                    listOfNotNull(files.kernelName, files.kernelBytes.takeIf { it > 0 }?.let(::formatBytes))
                        .joinToString(" · ")
                } else {
                    "未下载，安装时自动获取"
                },
            )
            InfoText(
                icon = MiuixIcons.Store,
                title = "OpenClash 插件",
                content = if (files.ipkReady) {
                    listOfNotNull(files.ipkName, files.ipkBytes.takeIf { it > 0 }?.let(::formatBytes))
                        .joinToString(" · ")
                } else {
                    "未下载，安装时自动获取"
                },
                bottomPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun InfoText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 24.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp),
            tint = colorScheme.onSurface,
        )
        Column {
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            Text(
                text = content,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun OutcomeRow(state: UiState, onViewLog: () -> Unit) {
    val outcome = state.outcome ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        ArrowPreference(
            title = "上次任务 · ${if (outcome.status == OpStatus.SUCCESS) "完成" else "未完成"}",
            summary = outcome.summary,
            startAction = {
                Icon(
                    imageVector = if (outcome.status == OpStatus.SUCCESS) MiuixIcons.Ok else MiuixIcons.Close2,
                    contentDescription = null,
                    tint = if (outcome.status == OpStatus.SUCCESS) AppStatus.success else colorScheme.error,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
            onClick = onViewLog,
        )
    }
}

@Composable
private fun EnvironmentCard(state: UiState, onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            state.envStatus.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (item.ok) MiuixIcons.Ok else MiuixIcons.Close2,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp),
                        tint = if (item.ok) AppStatus.success else colorScheme.error,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = MiuixTheme.textStyles.headline1.fontSize,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                        )
                        item.detail?.let {
                            Text(
                                text = it,
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = if (item.ok) "就绪" else "缺失",
                        fontSize = 14.sp,
                        color = if (item.ok) AppStatus.success else colorScheme.error,
                    )
                }
            }
            BasicComponent(
                title = "刷新检查",
                summary = "重新读取本地安装包状态",
                insideMargin = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
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
}

@Composable
private fun PackagesCard(
    state: UiState,
    onDownloadKernel: () -> Unit,
    onDownloadOpenclash: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = "Clash 内核",
            summary = if (state.downloadedFiles.kernelReady) {
                "已就绪 · 可重新下载"
            } else {
                "未下载 · 安装时自动获取"
            },
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Download,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onSurface,
                )
            },
            endActions = {
                TextButton(
                    text = if (state.downloadedFiles.kernelReady) "重新下载" else "下载",
                    onClick = onDownloadKernel,
                    enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            },
        )
        BasicComponent(
            title = "OpenClash 插件",
            summary = if (state.downloadedFiles.ipkReady) {
                "已就绪 · 可重新下载"
            } else {
                "未下载 · 安装时自动获取"
            },
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Store,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onSurface,
                )
            },
            endActions = {
                TextButton(
                    text = if (state.downloadedFiles.ipkReady) "重新下载" else "下载",
                    onClick = onDownloadOpenclash,
                    enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            },
        )
    }
}

@Composable
private fun ActionsCard(
    state: UiState,
    onInstall: () -> Unit,
    onTest: () -> Unit,
    onFan: () -> Unit,
    onUnlock: () -> Unit,
    onUsb: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
                minHeight = 48.dp,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("开始安装 OpenClash", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
                minHeight = 44.dp,
                colors = ButtonDefaults.buttonColors(
                    color = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurfaceContainer,
                ),
            ) {
                Text("测试路由器连接")
            }
        }
        ArrowPreference(
            title = "风扇控制",
            summary = "安装 GL.iNet 第三方风扇调速插件",
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = onFan,
            enabled = !state.busy,
        )
        ArrowPreference(
            title = "解锁隐藏菜单",
            summary = "解锁 GL 管理界面中被简体中文隐藏的菜单",
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Unlock,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = onUnlock,
            enabled = !state.busy,
        )
        ArrowPreference(
            title = "USB 共享网络修复",
            summary = "替换不兼容的 RNDIS 内核模块，重启后生效",
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = onUsb,
            enabled = !state.busy,
        )
    }
}

@Composable
private fun SupportLinks() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        ArrowPreference(
            title = "支持开发",
            summary = "给项目点一个 Star，或分享给需要的人",
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Promotions,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = {
                runCatching {
                    uriHandler.openUri("https://github.com/chenfa6666/gl-openclash-android-installer")
                }
            },
        )
        ArrowPreference(
            title = "了解 OpenClash",
            summary = "查看 OpenClash 官方文档与使用说明",
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Help,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = {
                runCatching { uriHandler.openUri("https://github.com/vernesong/OpenClash") }
            },
        )
    }
}

private fun com.chenfa.openclashinstaller.data.model.DownloadedFiles.kernelReadyCount(): Int = listOf(kernelReady, ipkReady).count { it }
