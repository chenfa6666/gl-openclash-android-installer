package com.chenfa.openclashinstaller.ui.theme

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/**
 * Liquid Glass 设计层（基于 AndroidLiquidGlass / io.github.kyant0:backdrop）。
 *
 * 结构：[LiquidGlassRoot] 里先放一块被 [layerBackdrop] 录制的「壁纸层」，
 * 所有玻璃面通过 [glass] / [Modifier.drawBackdrop] 采样这层内容做实时模糊；
 * Android 12（API 31）以上是真 RenderEffect 模糊，以下自动降级为高透叠色。
 */

/** 设备是否支持真正的背景模糊（RenderEffect）。不支持时玻璃面使用更不透明的叠色保证文字可读。 */
val SupportsBackdropBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** App 当前是否暗色主题的唯一真相入口（跟随系统，主题根部 provides）。 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** 当前窗口共享的玻璃采样层，由 [LiquidGlassRoot] / 对话框各自 provides。 */
val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop> {
    error("使用 glass 修饰符前需要先在 LiquidGlassRoot / 玻璃对话框内提供 LocalBackdrop")
}

/** 一套玻璃外观令牌（玻璃叠色、玻璃上的文字色、描边等）。 */
@Immutable
data class GlassTokens(
    val card: Color,
    val bar: Color,
    val dialog: Color,
    val primary: Color,
    val primaryDisabled: Color,
    val error: Color,
    val tonal: Color,
    val onGlass: Color,
    val onGlassEmphasis: Color,
    val onPrimary: Color,
    val sectionLabel: Color,
    val outline: Color,
    val fieldBorder: Color,
    val snack: Color,
    val dim: Color,
)

private val White = Color(0xFFFFFFFF)

val LightGlassTokens = GlassTokens(
    card = White.copy(alpha = if (SupportsBackdropBlur) 0.42f else 0.78f),
    bar = White.copy(alpha = if (SupportsBackdropBlur) 0.55f else 0.9f),
    dialog = White.copy(alpha = if (SupportsBackdropBlur) 0.5f else 0.88f),
    primary = PrimaryLight.copy(alpha = 0.82f),
    primaryDisabled = PrimaryLight.copy(alpha = 0.30f),
    error = ErrorLight.copy(alpha = 0.82f),
    tonal = White.copy(alpha = if (SupportsBackdropBlur) 0.30f else 0.68f),
    onGlass = OnSurfaceLight,
    onGlassEmphasis = OnSurfaceLight.copy(alpha = 0.72f),
    onPrimary = White,
    sectionLabel = OnSurfaceVariantLight,
    outline = OutlineLight,
    fieldBorder = Color(0xFF5B6B86),
    snack = White.copy(alpha = 0.82f),
    dim = Color.Black.copy(alpha = 0.22f),
)

val DarkGlassTokens = GlassTokens(
    card = if (SupportsBackdropBlur) White.copy(alpha = 0.10f) else Color(0xFF101A2E).copy(alpha = 0.76f),
    bar = if (SupportsBackdropBlur) White.copy(alpha = 0.14f) else Color(0xFF0D1626).copy(alpha = 0.84f),
    dialog = if (SupportsBackdropBlur) White.copy(alpha = 0.12f) else Color(0xFF101A2E).copy(alpha = 0.86f),
    primary = Color(0xFF6FA8FF).copy(alpha = 0.74f),
    primaryDisabled = White.copy(alpha = 0.08f),
    error = ErrorDark.copy(alpha = 0.72f),
    tonal = if (SupportsBackdropBlur) White.copy(alpha = 0.10f) else Color(0xFF17223A).copy(alpha = 0.74f),
    onGlass = OnSurfaceDark,
    onGlassEmphasis = OnSurfaceDark.copy(alpha = 0.72f),
    onPrimary = White,
    sectionLabel = OnSurfaceVariantDark,
    outline = OutlineDark,
    fieldBorder = Color(0xFF8FA6CC),
    snack = Color(0xFF1B2740).copy(alpha = 0.9f),
    dim = Color.Black.copy(alpha = 0.55f),
)

val LocalGlassTokens = staticCompositionLocalOf { LightGlassTokens }

/**
 * 把元素表面变成液态玻璃：采样共享背景层 → 模糊 + 提饱和 → 叠半透明色 → 边缘高光 + 柔阴影。
 *
 * 不改变元素本身的布局/点击逻辑，只负责「怎么画」。
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    tint: Color,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    shadowRadius: androidx.compose.ui.unit.Dp = 18.dp,
): Modifier {
    val backdrop = LocalBackdrop.current
    val dark = LocalDarkTheme.current
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { blurRadius.toPx() }
    val shadowColor = if (dark) Color(0x52000000) else Color(0x26000000)
    val highlightIntensity = if (dark) 0.28f else 0.5f
    val modifier = remember(
        backdrop, shape, tint, blurRadiusPx, shadowRadius, shadowColor, highlightIntensity,
    ) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadiusPx)
                vibrancy()
            },
            highlight = {
                Highlight(style = HighlightStyle.Default(intensity = highlightIntensity))
            },
            shadow = {
                Shadow(radius = shadowRadius, color = shadowColor)
            },
            onDrawSurface = { drawRect(tint) },
        )
    }
    return this.then(modifier)
}

/**
 * 液态玻璃根：背景壁纸层（被玻璃采样）+ 前景内容层（玻璃面画在这里）。
 * 两层是兄弟节点，避免玻璃自己录进采样层造成递归。
 */
@Composable
fun LiquidGlassRoot(
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit = { LiquidWallpaper() },
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            background()
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            content()
        }
    }
}

/**
 * 对话框窗口内的玻璃采样源：半透明深色 scrim + 顶部一点品牌色微光，
 * 让对话框玻璃面背后有东西可模糊（跨窗口无法采样 App 内容）。
 */
@Composable
fun DialogBackdrop(
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.42f,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color.Black.copy(alpha = scrimAlpha))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryLight.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.5f, -size.height * 0.08f),
                        radius = size.height * 0.95f,
                    ),
                )
            },
    )
}

/**
 * 液态壁纸：纵向柔和渐变 + 三个缓慢漂移的彩色光斑（蓝 / 紫 / 青），
 * 给玻璃模糊提供层次，呼应 launcher 蓝色品牌色。
 */
@Composable
fun LiquidWallpaper(modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val transition = rememberInfiniteTransition(label = "liquid-wallpaper")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    val baseStops = if (dark) {
        listOf(WallpaperDarkTop, WallpaperDarkMid, WallpaperDarkBottom)
    } else {
        listOf(WallpaperLightTop, WallpaperLightMid, WallpaperLightBottom)
    }
    val blobBlue = if (dark) WallpaperBlobBlueDark else WallpaperBlobBlueLight
    val blobViolet = if (dark) WallpaperBlobVioletDark else WallpaperBlobVioletLight
    val blobMint = if (dark) WallpaperBlobMintDark else WallpaperBlobMintLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = baseStops,
                        start = Offset.Zero,
                        end = Offset(0f, size.height),
                    ),
                )
                drawBlob(
                    color = blobBlue,
                    center = Offset(
                        size.width * (0.88f - 0.2f * progress),
                        size.height * (0.08f + 0.08f * progress),
                    ),
                    radius = size.minDimension * 0.78f,
                )
                drawBlob(
                    color = blobViolet,
                    center = Offset(
                        size.width * (0.08f + 0.18f * progress),
                        size.height * (0.9f - 0.12f * progress),
                    ),
                    radius = size.minDimension * 0.72f,
                )
                drawBlob(
                    color = blobMint,
                    center = Offset(
                        size.width * (0.5f + 0.22f * progress),
                        size.height * (0.52f + 0.1f * progress),
                    ),
                    radius = size.minDimension * 0.5f,
                )
            },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(
    color: Color,
    center: Offset,
    radius: Float,
) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
    )
}
