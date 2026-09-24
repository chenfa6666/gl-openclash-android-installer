package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 「内核」「openclash」并排蓝色凝胶玻璃按钮（视觉对称）。
 */
@Composable
fun DownloadButtonsRow(
    onKernel: () -> Unit,
    onOpenclash: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGlassTokens.current
    Row(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        GelGlassButton(
            text = "内核",
            onClick = onKernel,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            tint = tokens.primary,
            contentColor = tokens.onPrimary,
        )
        Spacer(Modifier.width(8.dp))
        GelGlassButton(
            text = "OpenClash",
            onClick = onOpenclash,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            tint = tokens.primary,
            contentColor = tokens.onPrimary,
        )
    }
}
