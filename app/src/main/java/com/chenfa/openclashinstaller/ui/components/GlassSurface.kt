package com.chenfa.openclashinstaller.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContentColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens
import com.chenfa.openclashinstaller.ui.theme.glass

/**
 * 玻璃面板：无点击行为，只负责液态玻璃外观 + 内容色传播。
 * 替代原来的 Material Card（布局/层级完全由调用方决定）。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.card,
    tint: Color? = null,
    contentColor: Color? = null,
    blurRadius: Dp = 24.dp,
    shadowRadius: Dp = 18.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val actualTint = tint ?: tokens.card
    Box(
        modifier = modifier.glass(
            shape = shape,
            tint = actualTint,
            blurRadius = blurRadius,
            shadowRadius = shadowRadius,
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (contentColor ?: tokens.onGlass),
        ) {
            // 只加内边距、不 fillMaxSize，保证短内容（对话框等）保持 wrap 自适应
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}

/**
 * 可点击玻璃面（玻璃按钮 / 整块可点卡片用）：
 * 按下时有轻微回弹缩放 + 叠色加深，禁用时整体变淡；不改变任何业务回调。
 */
@Composable
fun GlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassShapes.card,
    tint: Color,
    contentColor: Color,
    blurRadius: Dp = 24.dp,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "glassPress",
    )
    val alphaMultiplier = when {
        !enabled -> 0.4f
        pressed -> 0.88f
        else -> 1f
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(
                shape = shape,
                tint = tint.copy(alpha = tint.alpha * alphaMultiplier),
                blurRadius = blurRadius,
                shadowRadius = if (enabled) 10.dp else 6.dp,
            ),
        shape = shape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        interactionSource = source,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content,
        )
    }
}
