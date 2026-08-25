package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 主按钮：满宽。
 * 等价 Windows 版「连接测试」「开始安装」「强制结束」「安装风扇控制」按钮。
 *
 * @param variant 控制配色：
 *   - PRIMARY：填充主色（亮蓝，默认）
 *   - ERROR：填充 error 红色（强制结束）
 *   - TONAL：FilledTonal（填充较浅的 surface 色，gl 专属按钮次一级但不空）
 *   - TONAL_OUTLINE：描边次要（最轻量，很少用）
 * @param subtitle 可选小字说明，显示在按钮下一行或右下（用于「解锁隐藏功能」备注）
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
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val buttonMod = Modifier.fillMaxWidth().height(48.dp)
        when (variant) {
            PrimaryButtonVariant.PRIMARY -> Button(
                onClick = onClick, modifier = buttonMod, enabled = enabled,
            ) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

            PrimaryButtonVariant.ERROR -> Button(
                onClick = onClick, modifier = buttonMod, enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

            PrimaryButtonVariant.TONAL -> FilledTonalButton(
                onClick = onClick, modifier = buttonMod, enabled = enabled,
            ) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

            PrimaryButtonVariant.TONAL_OUTLINE -> OutlinedButton(
                onClick = onClick, modifier = buttonMod, enabled = enabled,
            ) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

