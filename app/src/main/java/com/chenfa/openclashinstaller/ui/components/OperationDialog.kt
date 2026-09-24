package com.chenfa.openclashinstaller.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LiquidWallpaper
import com.chenfa.openclashinstaller.ui.theme.LocalBackdrop
import com.chenfa.openclashinstaller.ui.theme.LocalDarkTheme
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens
import com.chenfa.openclashinstaller.ui.theme.LogBg
import com.chenfa.openclashinstaller.ui.theme.LogBgDark
import com.chenfa.openclashinstaller.ui.theme.LogNormal
import com.chenfa.openclashinstaller.ui.theme.LogNormalDark
import com.chenfa.openclashinstaller.ui.theme.glass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 操作弹窗（像设置对话框一样，但全屏，液态玻璃版）：
 *  - 顶部玻璃 TopAppBar：标题 + 关闭按钮（busy 时禁用）
 *  - 中部：日志列表（ProgressLog，进度原地刷新，自动滚到底），保留实体底色保证可读性
 *  - 底部：强制结束按钮（busy 时红色凝胶，否则蓝色凝胶「关闭」）
 *
 * 等价 Windows 版独立操作面板，把"日志 + 强制结束"合并到一个独立界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationDialog(
    title: String,
    entries: List<LogEntry>,
    busy: Boolean,
    onClose: () -> Unit,
    onAbort: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BackHandler(enabled = !busy) { onClose() }
        val backdrop = rememberLayerBackdrop()
        val tokens = LocalGlassTokens.current
        val dark = LocalDarkTheme.current

        Box(modifier = Modifier.fillMaxSize()) {
            // 采样源：液态壁纸 + 压暗遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                LiquidWallpaper()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.dim),
                )
            }

            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = { Text(title, color = tokens.onGlass) },
                            navigationIcon = {
                                IconButton(onClick = onClose, enabled = !busy) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = tokens.onGlass,
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                                titleContentColor = tokens.onGlass,
                                navigationIconContentColor = tokens.onGlass,
                                actionIconContentColor = tokens.onGlass,
                            ),
                            modifier = Modifier.glass(
                                shape = GlassShapes.bar,
                                tint = tokens.bar,
                                blurRadius = 20.dp,
                                shadowRadius = 12.dp,
                            ),
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 日志区（占大部分，保留实体底色保证等宽日志可读性）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(GlassShapes.logPanel)
                                .background(if (dark) LogBgDark else LogBg),
                        ) {
                            if (entries.isEmpty()) {
                                Text(
                                    "（暂无日志）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dark) LogNormalDark else LogNormal,
                                    modifier = Modifier.padding(8.dp),
                                )
                            } else {
                                ProgressLog(entries = entries)
                            }
                        }

                        // 底部：强制结束或关闭
                        if (busy) {
                            PrimaryButton(
                                text = "强制结束安装",
                                onClick = onAbort,
                                variant = PrimaryButtonVariant.ERROR,
                            )
                        } else {
                            PrimaryButton(
                                text = "关闭",
                                onClick = onClose,
                            )
                        }
                    }
                }
            }
        }
    }
}
