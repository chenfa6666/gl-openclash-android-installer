package com.chenfa.openclashinstaller.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    secondary = PrimaryLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    secondary = PrimaryDark,
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
            // 状态栏/导航栏统一跟应用 background（surface）一致，不再用 primary 撞色
            val surfaceArgb = colors.background.toArgb()
            window.statusBarColor = surfaceArgb
            if (Build.VERSION.SDK_INT >= 27) {
                window.navigationBarColor = surfaceArgb
            }
            val controller = WindowCompat.getInsetsController(window, view)
            // 状态栏/导航栏图标色：亮色主题（亮 surface）→ 黑；暗色主题（暗 surface）→ 白
            controller.isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= 26) {
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            // 取消系统栏强制遮罩，做到真正的「颜色跟应用融为一体」
            if (Build.VERSION.SDK_INT >= 29) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}


