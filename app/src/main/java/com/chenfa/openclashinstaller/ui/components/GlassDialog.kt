package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chenfa.openclashinstaller.ui.theme.DialogBackdrop
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalBackdrop
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 液态玻璃对话框：替代 Material AlertDialog。
 *
 * 对话框是独立窗口，无法采样 App 内容，因此自带一层「深色 scrim + 品牌微光」作为玻璃采样源，
 * 面板本身是模糊这块采样层的玻璃。参数与原 AlertDialog 用法一一对应，业务逻辑不变。
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    title: String? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            // scrim 点击由本组件自己处理（避免面板边缘误触）
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val backdrop = rememberLayerBackdrop()
        val tokens = LocalGlassTokens.current
        val maxPanelHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f

        Box(modifier = Modifier.fillMaxSize()) {
            // 采样源：scrim + 微光
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                DialogBackdrop()
            }

            // 点外部关闭
            if (dismissOnClickOutside) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissRequest,
                        ),
                )
            }

            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GlassSurface(
                        modifier = modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .heightIn(max = maxPanelHeight),
                        shape = GlassShapes.dialog,
                        tint = tokens.dialog,
                        blurRadius = 28.dp,
                        shadowRadius = 32.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 24.dp,
                            top = 22.dp,
                            end = 24.dp,
                            bottom = 18.dp,
                        ),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (title != null) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.onGlass,
                                )
                            }
                            // 短内容自适应高度；长内容（如设置）在此区域内滚动，标题与按钮始终可见
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                            ) {
                                content()
                            }
                            if (confirmButton != null || dismissButton != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    dismissButton?.invoke()
                                    confirmButton?.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
