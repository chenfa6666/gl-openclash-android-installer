package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 主按钮：满宽。
 * 等价 Windows 版「连接测试」「开始安装」「强制结束」「安装风扇控制」按钮。
 *
 * @param variant 控制配色：PRIMARY 主色、ERROR 红色（强制结束）、TONAL_OUTLINE 描边次要。
 */
enum class PrimaryButtonVariant { PRIMARY, ERROR, TONAL_OUTLINE }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PrimaryButtonVariant = PrimaryButtonVariant.PRIMARY,
) {
    val m = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .height(48.dp)
    when (variant) {
        PrimaryButtonVariant.PRIMARY -> Button(
            onClick = onClick, modifier = m, enabled = enabled,
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }

        PrimaryButtonVariant.ERROR -> Button(
            onClick = onClick, modifier = m, enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }

        PrimaryButtonVariant.TONAL_OUTLINE -> OutlinedButton(
            onClick = onClick, modifier = m, enabled = enabled,
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }
    }
}
