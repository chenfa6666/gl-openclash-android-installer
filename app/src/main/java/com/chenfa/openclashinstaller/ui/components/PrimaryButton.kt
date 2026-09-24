package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 主按钮：满宽液态凝胶玻璃按钮。
 * 等价 Windows 版「连接测试」「开始安装」「强制结束」「安装风扇控制」按钮。
 *
 * @param variant 控制配色：
 *   - PRIMARY：蓝色凝胶（默认）
 *   - ERROR：红色凝胶（强制结束）
 *   - TONAL：中性玻璃（gl 专属按钮次一级但不空）
 *   - TONAL_OUTLINE：描边次要（最轻量，很少用）
 * @param subtitle 可选小字说明，显示在按钮下一行（用于「解锁隐藏功能」备注）
 */
enum class PrimaryButtonVariant { PRIMARY, ERROR, TONAL, TONAL_OUTLINE }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PrimaryButtonVariant = PrimaryButtonVariant.PRIMARY,
    subtitle: String? = null,
) {
    val tokens = LocalGlassTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val buttonMod = Modifier
            .fillMaxWidth()
            .height(48.dp)
        when (variant) {
            PrimaryButtonVariant.PRIMARY -> GelGlassButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                modifier = buttonMod,
                tint = tokens.primary,
                contentColor = tokens.onPrimary,
            )

            PrimaryButtonVariant.ERROR -> GelGlassButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                modifier = buttonMod,
                tint = tokens.error,
                contentColor = tokens.onPrimary,
            )

            PrimaryButtonVariant.TONAL -> GelGlassButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                modifier = buttonMod,
                tint = tokens.tonal,
                contentColor = tokens.onGlass,
            )

            PrimaryButtonVariant.TONAL_OUTLINE -> OutlinedButton(
                onClick = onClick,
                modifier = buttonMod,
                enabled = enabled,
                shape = GlassShapes.button,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = tokens.onGlass,
                ),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, tokens.outline),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onGlassEmphasis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** 单个液态凝胶玻璃按钮：按下有回弹，禁用由 [GlassSurface] 统一变淡。 */
@Composable
internal fun GelGlassButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    tint: Color,
    contentColor: Color,
) {
    GlassSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = GlassShapes.button,
        tint = tint,
        contentColor = contentColor,
        blurRadius = 16.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
