package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.EnvItem
import com.chenfa.openclashinstaller.ui.theme.EnvNoColor
import com.chenfa.openclashinstaller.ui.theme.EnvOkColor

/**
 * 单行环境检查：name + 绿 yes / 红 no。
 * 等价 Windows 版左侧顶部 3 行环境检查。
 */
@Composable
fun EnvCheckRow(item: EnvItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (item.ok) "yes" else "no",
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.ok) EnvOkColor else EnvNoColor,
        )
    }
}
