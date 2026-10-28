package com.chenfa.openclashinstaller.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.chenfa.openclashinstaller.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 业务状态色。
 *
 * KernelSU 的绿色状态卡使用固定的深绿/浅绿组合，不跟随动态取色，
 * 这样在纯黑背景上仍有稳定的辨识度。
 */
@Immutable
data class AppStatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val LightStatusColors = AppStatusColors(
    success = Color(0xFF248A3D),
    successContainer = Color(0xFFDFFAE4),
    onSuccessContainer = Color(0xFF164A29),
    warning = Color(0xFFB25000),
    warningContainer = Color(0xFFFFF1D6),
    onWarningContainer = Color(0xFF4A2C00),
)

private val DarkStatusColors = AppStatusColors(
    success = Color(0xFF36D167),
    successContainer = Color(0xFF1A3825),
    onSuccessContainer = Color(0xFFDFFAE4),
    warning = Color(0xFFFF9F0A),
    warningContainer = Color(0xFF3A2A0A),
    onWarningContainer = Color(0xFFFFE2A8),
)

private val LocalAppStatusColors = staticCompositionLocalOf { LightStatusColors }
private val LocalAppIsDark = staticCompositionLocalOf { false }

val AppStatus: AppStatusColors
    @Composable @ReadOnlyComposable
    get() = LocalAppStatusColors.current

val appIsDarkTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalAppIsDark.current

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = appIsDarkTheme

/** 与 KernelSU 一致的外观开关，由 MainScreen 根据设置提供。 */
val LocalEnableBlur = staticCompositionLocalOf { false }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }

/**
 * 应用主题：按 KernelSU 的 Miuix 外观实现。
 *
 * - 跟随系统 / 浅色 / 深色三档
 * - 可选 Monet 动态取色（关闭时使用 Miuix 默认蓝）
 * - 深浅色下都使用纯黑/纯白背景 + 分组卡片
 */
@Composable
fun OpenClashTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    monetEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val controller = ThemeController(
        colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> if (monetEnabled) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
            ThemeMode.LIGHT -> if (monetEnabled) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
            ThemeMode.DARK -> if (monetEnabled) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        },
        isDark = dark,
    )

    MiuixTheme(controller = controller) {
        val actualDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
        LaunchedEffect(actualDark, context) {
            val window = (context as? Activity)?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !actualDark
                isAppearanceLightNavigationBars = !actualDark
            }
        }
        CompositionLocalProvider(
            LocalAppIsDark provides actualDark,
            LocalAppStatusColors provides if (actualDark) DarkStatusColors else LightStatusColors,
        ) {
            content()
        }
    }
}
