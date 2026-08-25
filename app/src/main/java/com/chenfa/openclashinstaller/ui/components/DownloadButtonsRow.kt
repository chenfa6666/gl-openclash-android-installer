package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 「下载内核」「下载 openclash」并排按钮。
 * 等价 Windows 版左侧顶部两按钮一行显示。
 */
@Composable
fun DownloadButtonsRow(
    onKernel: () -> Unit,
    onOpenclash: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Button(
            onClick = onKernel,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) { Text("下载内核", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onOpenclash,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) { Text("下载 openclash", style = MaterialTheme.typography.labelLarge) }
    }
}
