package com.chenfa.openclashinstaller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens
import com.chenfa.openclashinstaller.ui.theme.glass

/** 底部三个主页面。 */
enum class LiquidTab(val label: String, val icon: ImageVector) {
    INSTALL("安装", Icons.Outlined.DeveloperBoard),
    SETTINGS("设置", Icons.Filled.Settings),
    ABOUT("关于", Icons.Filled.Info),
}

/**
 * 悬浮液态玻璃底栏：一块漂浮在壁纸之上的胶囊玻璃，
 * 内含 安装 / 设置 / 关于 三个切换项；选中项有品牌色凝胶胶囊 + 着色图标文字。
 */
@Composable
fun LiquidBottomBar(
    selected: LiquidTab,
    onSelect: (LiquidTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGlassTokens.current
    val barShape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .glass(
                shape = barShape,
                tint = tokens.bar,
                blurRadius = 22.dp,
                shadowRadius = 18.dp,
            )
            .height(68.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LiquidTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) tokens.onPrimary else tokens.onGlassEmphasis,
                    label = "tabColor",
                )
                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) tokens.primary else Color.Transparent,
                    label = "tabPill",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // 选中项背后的凝胶胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillColor)
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = itemColor,
                                modifier = Modifier.height(22.dp),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = itemColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
