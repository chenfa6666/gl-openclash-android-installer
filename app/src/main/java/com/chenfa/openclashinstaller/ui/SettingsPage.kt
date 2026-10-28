package com.chenfa.openclashinstaller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.Settings
import com.chenfa.openclashinstaller.data.model.ThemeMode
import com.chenfa.openclashinstaller.ui.components.BlurredBar
import com.chenfa.openclashinstaller.ui.components.InfoBanner
import com.chenfa.openclashinstaller.ui.components.StatusTone
import com.chenfa.openclashinstaller.ui.components.rememberBlurBackdrop
import com.chenfa.openclashinstaller.ui.theme.LocalEnableBlur
import com.chenfa.openclashinstaller.ui.theme.appIsDarkTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 「设置」页：主题设置、连接与下载二级页、分享日志、关于二级页。
 */
@Composable
fun SettingsPage(
    vm: MainViewModel,
    bottomPadding: Dp,
    baseDensity: Density,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    var showThemeSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var detailPage by remember { mutableStateOf<SettingsDetail?>(null) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "设置",
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
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = "主题设置",
                                summary = state.themeMode.label +
                                        if (state.monetEnabled) " · Monet 已启用" else " · 固定蓝色",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Theme,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { showThemeSettings = true },
                            )
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = "连接参数",
                                summary = if (state.fields.isComplete()) {
                                    "${state.fields.user.trim()}@${state.fields.ip.trim()}:${state.fields.port.trim()}"
                                } else {
                                    "未完善"
                                },
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Link,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { detailPage = SettingsDetail.CONNECTION },
                            )
                            ArrowPreference(
                                title = "下载地址",
                                summary = "Clash 内核 / OpenClash 插件 / 风扇插件",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Download,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { detailPage = SettingsDetail.DOWNLOAD },
                            )
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = "分享运行日志",
                                summary = "通过系统分享面板发送完整日志",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Share,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = vm::shareLog,
                            )
                            ArrowPreference(
                                title = "关于",
                                summary = "版本 ${com.chenfa.openclashinstaller.BuildConfig.VERSION_NAME}",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { showAbout = true },
                            )
                        }

                        Spacer(Modifier.height(bottomPadding + 12.dp))
                    }
                }
            }
        }
    }

    detailPage?.let { page ->
        SettingsDetailDialog(
            page = page,
            vm = vm,
            baseDensity = baseDensity,
            onDismiss = { detailPage = null },
        )
    }
    if (showThemeSettings) {
        ThemeSettingsDialog(
            themeMode = state.themeMode,
            monetEnabled = state.monetEnabled,
            blurEnabled = state.blurEnabled,
            floatingBottomBar = state.floatingBottomBar,
            glassEnabled = state.glassEnabled,
            pageSwitchGesture = state.pageSwitchGesture,
            uiScale = state.uiScale,
            baseDensity = baseDensity,
            onThemeModeChange = vm::setThemeMode,
            onMonetChange = vm::setMonetEnabled,
            onBlurChange = vm::setBlurEnabled,
            onFloatingBottomBarChange = vm::setFloatingBottomBar,
            onGlassChange = vm::setGlassEnabled,
            onPageSwitchGestureChange = vm::setPageSwitchGesture,
            onUiScaleChange = vm::setUiScale,
            onDismiss = { showThemeSettings = false },
        )
    }

    if (showAbout) {
        Dialog(
            onDismissRequest = { showAbout = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = baseDensity.density * state.uiScale,
                    fontScale = baseDensity.fontScale * state.uiScale,
                ),
            ) {
                BackHandler { showAbout = false }
                AboutPage(
                    bottomPadding = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                    onBack = { showAbout = false },
                )
            }
        }
    }
}

private enum class SettingsDetail { CONNECTION, DOWNLOAD }

private enum class ParamField(
    val title: String,
    val keyboardType: KeyboardType,
    val isConnection: Boolean,
) {
    IP("IP 地址", KeyboardType.Uri, true),
    USER("用户名", KeyboardType.Text, true),
    PASSWORD("密码", KeyboardType.Password, true),
    PORT("SSH 端口", KeyboardType.Number, true),
    KERNEL_URL("Clash 内核 URL", KeyboardType.Uri, false),
    OPENCLASH_URL("OpenClash 插件 URL", KeyboardType.Uri, false),
    FAN_URL("风扇控制插件 URL", KeyboardType.Uri, false),
}

/** 连接参数与下载地址的二级页面。 */
@Composable
private fun SettingsDetailDialog(
    page: SettingsDetail,
    vm: MainViewModel,
    baseDensity: Density,
    onDismiss: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    var editing by remember { mutableStateOf<ParamField?>(null) }
    var draft by remember { mutableStateOf("") }

    fun valueOf(field: ParamField): String = when (field) {
        ParamField.IP -> state.fields.ip
        ParamField.USER -> state.fields.user
        ParamField.PASSWORD -> state.fields.password
        ParamField.PORT -> state.fields.port
        ParamField.KERNEL_URL -> state.kernelUrl
        ParamField.OPENCLASH_URL -> state.openclashUrl
        ParamField.FAN_URL -> state.fanUrl
    }

    fun open(field: ParamField) {
        editing = field
        draft = valueOf(field)
    }

    fun saveParameter(field: ParamField, raw: String): String? {
        return if (field.isConnection) {
            val value = if (field == ParamField.PORT) raw.filter(Char::isDigit).take(5) else raw
            val candidate = when (field) {
                ParamField.IP -> state.fields.copy(ip = value.trim())
                ParamField.USER -> state.fields.copy(user = value.trim())
                ParamField.PASSWORD -> state.fields.copy(password = value)
                ParamField.PORT -> state.fields.copy(port = value)
                else -> state.fields
            }.normalized()
            candidate.firstError()?.let { return it }
            if (!vm.saveConnectionFields(candidate)) "保存失败，请稍后重试" else null
        } else {
            val k = if (field == ParamField.KERNEL_URL) raw.trim() else state.kernelUrl
            val o = if (field == ParamField.OPENCLASH_URL) raw.trim() else state.openclashUrl
            val f = if (field == ParamField.FAN_URL) raw.trim() else state.fanUrl
            Settings(kernelUrl = k, openclashUrl = o, fanUrl = f, fields = state.fields)
                .firstUrlError()?.let { return it }
            if (!vm.saveDownloadUrls(k, o, f)) "保存失败，请稍后重试" else null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * state.uiScale,
                fontScale = baseDensity.fontScale * state.uiScale,
            ),
        ) {
        BackHandler {
            if (editing != null) editing = null else onDismiss()
        }
        Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.surface) {
            Scaffold(
                topBar = {
                    BlurredBar(backdrop = backdrop) {
                        TopAppBar(
                            color = barColor,
                            title = if (page == SettingsDetail.CONNECTION) "连接参数" else "下载地址",
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss,
                                    minWidth = 48.dp,
                                    minHeight = 48.dp,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = "返回设置",
                                        tint = colorScheme.onBackground,
                                        modifier = Modifier.size(22.dp),
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
                                if (page == SettingsDetail.CONNECTION) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    ArrowPreference(
                                        title = "IP 地址",
                                        summary = state.fields.ip,
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Location,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.IP) },
                                    )
                                    ArrowPreference(
                                        title = "用户名",
                                        summary = state.fields.user,
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Contacts,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.USER) },
                                    )
                                    ArrowPreference(
                                        title = "密码",
                                        summary = if (state.fields.password.isEmpty()) "未设置" else "••••••••",
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.PASSWORD) },
                                    )
                                    ArrowPreference(
                                        title = "SSH 端口",
                                        summary = state.fields.port,
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.WorldClock,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.PORT) },
                                    )
                                    ArrowPreference(
                                        title = "测试连接",
                                        summary = "验证 SSH 账号、端口与密码",
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Link,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = vm::testConn,
                                        enabled = !state.busy,
                                    )
                                }
                                }

                                if (page == SettingsDetail.DOWNLOAD) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    ArrowPreference(
                                        title = "Clash 内核 URL",
                                        summary = state.kernelUrl.shortenedUrl(),
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Download,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.KERNEL_URL) },
                                    )
                                    ArrowPreference(
                                        title = "OpenClash 插件 URL",
                                        summary = state.openclashUrl.shortenedUrl(),
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Store,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.OPENCLASH_URL) },
                                    )
                                    ArrowPreference(
                                        title = "风扇控制插件 URL",
                                        summary = state.fanUrl.shortenedUrl(),
                                        startAction = {
                                            Icon(
                                                imageVector = MiuixIcons.Tune,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        onClick = { open(ParamField.FAN_URL) },
                                    )
                                }

                                TextButton(
                                    text = "恢复默认下载地址",
                                    onClick = {
                                        vm.saveDownloadUrls(
                                            Constants.DEF_URL_KERNEL,
                                            Constants.DEF_URL_OPENCLASH,
                                            Constants.DEF_URL_FAN,
                                        )
                                    },
                                    minWidth = 0.dp,
                                    minHeight = 34.dp,
                                    insideMargin = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                                }

                                Spacer(
                                    Modifier.height(
                                        WindowInsets.navigationBars.asPaddingValues()
                                            .calculateBottomPadding() + 12.dp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            editing?.let { field ->
                ParameterEditorOverlay(
                    title = field.title,
                    initialValue = draft,
                    keyboardType = field.keyboardType,
                    onDismiss = { editing = null },
                    onSave = { saveParameter(field, it) },
                )
            }
        }
    }
    }
    }
}

@Composable
private fun ParameterEditorOverlay(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onSave: (String) -> String?,
) {
    var value by remember(title) { mutableStateOf(initialValue) }
    var error by remember(title) { mutableStateOf<String?>(null) }
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                ) { },
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceContainer,
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                )
                error?.let {
                    Text(
                        text = it,
                        color = colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "保存",
                        onClick = {
                            val result = onSave(value)
                            if (result == null) onDismiss() else error = result
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }

/**
 * KernelSU「主题设置」页：大标题 + 预测览 + 三档 TabRow + Monet 开关。
 */
@Composable
private fun ThemeSettingsDialog(
    themeMode: ThemeMode,
    monetEnabled: Boolean,
    blurEnabled: Boolean,
    floatingBottomBar: Boolean,
    glassEnabled: Boolean,
    pageSwitchGesture: Boolean,
    uiScale: Float,
    baseDensity: Density,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMonetChange: (Boolean) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    onFloatingBottomBarChange: (Boolean) -> Unit,
    onGlassChange: (Boolean) -> Unit,
    onPageSwitchGestureChange: (Boolean) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember(uiScale) { mutableFloatStateOf(uiScale) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * uiScale,
                fontScale = baseDensity.fontScale * uiScale,
            ),
        ) {
        BackHandler(onBack = onDismiss)
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.surface) {
            val scrollBehavior = MiuixScrollBehavior()
            Scaffold(
                topBar = {
                    TopAppBar(
                        color = colorScheme.surface,
                        title = "主题设置",
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                minWidth = 48.dp,
                                minHeight = 48.dp,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "返回",
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                },
                popupHost = { },
                contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .padding(horizontal = 12.dp),
                    contentPadding = innerPadding,
                    overscrollEffect = null,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ThemePreview(modifier = Modifier.padding(top = 8.dp))
                    }
                    item {
                        TabRow(
                            tabs = listOf("跟随系统", "浅色", "深色"),
                            selectedTabIndex = themeMode.ordinal,
                            onTabSelected = { index ->
                                onThemeModeChange(ThemeMode.entries[index])
                            },
                        )
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = "启用 Monet 颜色",
                                summary = "从壁纸中提取强调色",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Theme,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = monetEnabled,
                                onCheckedChange = onMonetChange,
                            )
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = "模糊",
                                summary = "启用顶栏和底栏的真实背景模糊",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = blurEnabled,
                                onCheckedChange = onBlurChange,
                            )
                            SwitchPreference(
                                title = "悬浮底栏",
                                summary = "使用 Apple 风格悬浮底栏，窄屏生效",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = floatingBottomBar,
                                onCheckedChange = onFloatingBottomBarChange,
                            )
                            if (floatingBottomBar) {
                                SwitchPreference(
                                    title = "液态玻璃",
                                    summary = "为悬浮底栏启用半透明高光玻璃质感",
                                    startAction = {
                                        Icon(
                                            imageVector = MiuixIcons.Layers,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = colorScheme.onBackground,
                                        )
                                    },
                                    checked = glassEnabled,
                                    onCheckedChange = onGlassChange,
                                )
                            }
                            SwitchPreference(
                                title = "页面滑动切换",
                                summary = "允许左右滑动切换安装、设置、关于",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.HorizontalSplit,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = pageSwitchGesture,
                                onCheckedChange = onPageSwitchGestureChange,
                            )
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = "界面缩放",
                                summary = "调整页面内容的全局显示比例",
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                endActions = {
                                    Text(
                                        text = "${(sliderValue * 100).toInt()}%",
                                        color = colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                bottomAction = {
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = { sliderValue = it },
                                        onValueChangeFinished = { onUiScaleChange(sliderValue) },
                                        valueRange = 0.8f..1.1f,
                                        steps = 5,
                                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                    )
                                },
                            )
                        }
                    }
                    item {
                        Text(
                            text = "主题设置会立即生效并自动保存，无需重启应用。",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    item {
                        Spacer(
                            Modifier.height(
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() + 12.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ThemePreview(modifier: Modifier = Modifier) {
    val dark = appIsDarkTheme
    val previewBg = if (dark) Color.Black else Color(0xFFF2F2F7)
    val previewCard = colorScheme.surfaceContainer
    val previewGreen = if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .height(178.dp),
            shape = RoundedCornerShape(18.dp),
            color = previewBg,
            border = BorderStroke(1.dp, colorScheme.dividerLine),
        ) {
            Row(modifier = Modifier.padding(10.dp)) {
                Column(
                    modifier = Modifier.width(30.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = if (index == 0) previewCard else colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(5.dp),
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "OpenClash",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(previewGreen, RoundedCornerShape(8.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(previewCard, RoundedCornerShape(8.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(previewCard, RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

private fun String.shortenedUrl(maxLength: Int = 52): String =
    if (length <= maxLength) this else take(maxLength) + "…"
