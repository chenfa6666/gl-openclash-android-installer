package com.chenfa.openclashinstaller.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenfa.openclashinstaller.data.model.UiEvent
import com.chenfa.openclashinstaller.ui.components.BlurredBar
import com.chenfa.openclashinstaller.ui.components.FloatingBottomBar
import com.chenfa.openclashinstaller.ui.components.FloatingBottomBarItem
import com.chenfa.openclashinstaller.ui.components.NoticeHost
import com.chenfa.openclashinstaller.ui.components.NoticeUi
import com.chenfa.openclashinstaller.ui.components.OperationDialog
import com.chenfa.openclashinstaller.ui.components.durationMs
import com.chenfa.openclashinstaller.ui.components.rememberBlurBackdrop
import com.chenfa.openclashinstaller.ui.theme.LocalEnableBlur
import com.chenfa.openclashinstaller.ui.theme.LocalEnableFloatingBottomBar
import com.chenfa.openclashinstaller.ui.theme.LocalEnableFloatingBottomBarBlur
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    ENVIRONMENT("环境", MiuixIcons.Tune),
    INSTALL("安装", MiuixIcons.Download),
    SETTINGS("设置", MiuixIcons.Settings),
}

/**
 * KernelSU Miuix 主框架。
 *
 * 注意：导航栏/悬浮底栏切换时保持 pager 在同一组合位置，
 * 避免切换布局分支时销毁设置页内部的主题/关于/连接弹窗。
 */
@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<NoticeUi?>(null) }
    var noticeId by remember { mutableLongStateOf(0L) }
    val pagerState = rememberPagerState(pageCount = { AppTab.entries.size })
    val scope = rememberCoroutineScope()
    val blurBackdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val surfaceColor = colorScheme.surface
    val floatingBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    LaunchedEffect(Unit) { vm.init0() }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Notice -> {
                    val id = ++noticeId
                    notice = NoticeUi(
                        id = id,
                        message = event.message,
                        level = event.level,
                        durationMs = event.level.durationMs(),
                    )
                }

                UiEvent.SettingsSaved -> scope.launch { pagerState.animateScrollToPage(AppTab.INSTALL.ordinal) }
            }
        }
    }

    LaunchedEffect(notice?.id) {
        val current = notice ?: return@LaunchedEffect
        delay(current.durationMs)
        if (notice?.id == current.id) notice = null
    }

    CompositionLocalProvider(
        LocalEnableBlur provides state.blurEnabled,
        LocalEnableFloatingBottomBar provides state.floatingBottomBar,
        LocalEnableFloatingBottomBarBlur provides state.glassEnabled,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val floatingBar = state.floatingBottomBar
            val useRail = !floatingBar &&
                    (maxWidth >= 840.dp || (maxWidth >= 600.dp && maxHeight / maxWidth < 1.2f))
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val glassActive = state.glassEnabled && Build.VERSION.SDK_INT >= 33

            Scaffold(
                containerColor = colorScheme.surface,
                bottomBar = {
                    if (!useRail && !floatingBar) {
                        BlurredBar(backdrop = blurBackdrop) {
                            NavigationBar(
                                color = if (blurBackdrop != null) Color.Transparent else colorScheme.surface,
                                showDivider = false,
                            ) {
                                AppTab.entries.forEachIndexed { index, tab ->
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        icon = tab.icon,
                                        label = tab.label,
                                    )
                                }
                            }
                        }
                    }
                },
                snackbarHost = { NoticeHost(notice = notice, onDismiss = { notice = null }) },
            ) { innerPadding ->
                val bottomPadding = when {
                    floatingBar -> navBarBottom + 96.dp
                    useRail -> navBarBottom
                    else -> innerPadding.calculateBottomPadding()
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // 固定占位，切换 rail 显隐时不改变 pager 在 Row 中的位置，
                    // 避免主题设置等二级弹窗被销毁。
                    Box(
                        modifier = Modifier
                            .width(if (useRail) 88.dp else 0.dp)
                            .fillMaxHeight(),
                    ) {
                        if (useRail) {
                            NavigationRail(
                                color = colorScheme.surface,
                                showDivider = false,
                            ) {
                                AppTab.entries.forEachIndexed { index, tab ->
                                    NavigationRailItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        icon = tab.icon,
                                        label = tab.label,
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (floatingBar && glassActive) {
                                        Modifier.layerBackdrop(floatingBackdrop)
                                    } else {
                                        Modifier
                                    },
                                ),
                            userScrollEnabled = state.pageSwitchGesture,
                        ) { page ->
                            PageContent(
                                page = page,
                                vm = vm,
                                bottomPadding = bottomPadding,
                                uiScale = state.uiScale,
                                onOpenSettings = {
                                    scope.launch { pagerState.animateScrollToPage(AppTab.SETTINGS.ordinal) }
                                },
                                onOpenEnvironment = {
                                    scope.launch { pagerState.animateScrollToPage(AppTab.ENVIRONMENT.ordinal) }
                                },
                            )
                        }

                        if (floatingBar) {
                            FloatingBottomBar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        start = 28.dp,
                                        end = 28.dp,
                                        bottom = navBarBottom + 14.dp,
                                    ),
                                selectedIndex = pagerState.currentPage,
                                onSelected = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                backdrop = floatingBackdrop,
                                tabsCount = AppTab.entries.size,
                                isBlurEnabled = glassActive,
                            ) { activateTab ->
                                AppTab.entries.forEachIndexed { index, tab ->
                                    FloatingBottomBarItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = { activateTab(index) },
                                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.operationOpen) {
        OperationDialog(
            title = state.operationTitle,
            entries = state.logEntries,
            busy = state.busy,
            aborting = state.aborting,
            confirmAbortOpen = state.confirmAbortOpen,
            startedAt = state.operationStartedAt,
            outcome = state.outcome,
            onClose = vm::closeOperation,
            onAbortRequest = vm::openConfirmAbort,
            onConfirmAbort = vm::abort,
            onDismissAbort = vm::closeConfirmAbort,
            onRetry = vm::retry,
            onShareLog = vm::shareLog,
        )
    }
}

@Composable
private fun PageContent(
    page: Int,
    vm: MainViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp,
    uiScale: Float,
    onOpenSettings: () -> Unit,
    onOpenEnvironment: () -> Unit,
) {
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale,
        ),
    ) {
        when (page) {
            AppTab.ENVIRONMENT.ordinal -> EnvironmentPage(
                vm = vm,
                bottomPadding = bottomPadding,
                onOpenSettings = onOpenSettings,
            )

            AppTab.INSTALL.ordinal -> InstallPage(
                vm = vm,
                bottomPadding = bottomPadding,
                onOpenSettings = onOpenSettings,
                onOpenEnvironment = onOpenEnvironment,
            )

            AppTab.SETTINGS.ordinal -> SettingsPage(
                vm = vm,
                bottomPadding = bottomPadding,
                baseDensity = baseDensity,
            )
        }
    }
}
