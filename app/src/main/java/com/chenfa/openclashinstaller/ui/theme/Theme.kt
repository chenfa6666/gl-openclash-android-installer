package com.chenfa.openclashinstaller.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    secondary = PrimaryLight,
    background = WallpaperLightMid,
    onBackground = OnSurfaceLight,
    surface = WallpaperLightMid,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFF5B6B86),
    error = ErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF061226),
    secondary = PrimaryDark,
    background = WallpaperDarkMid,
    onBackground = OnSurfaceDark,
    surface = WallpaperDarkMid,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF8FA6CC),
    error = ErrorDark,
    onError = Color(0xFF2A060A),
)

@Composable
fun OpenClashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 液态壁纸全屏铺底，系统栏透明融入
            window.statusBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= 27) {
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            val controller = WindowCompat.getInsetsController(window, view)
            // 状态栏/导航栏图标色：亮色主题（亮壁纸）→ 黑；暗色主题（暗壁纸）→ 白
            controller.isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= 26) {
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            // 取消系统栏强制遮罩，做到真正的「壁纸与玻璃融为一体」
            if (Build.VERSION.SDK_INT >= 29) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalGlassTokens provides if (darkTheme) DarkGlassTokens else LightGlassTokens,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
