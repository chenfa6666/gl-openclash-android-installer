package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenfa.openclashinstaller.ui.components.CollapsibleCard
import com.chenfa.openclashinstaller.ui.components.DownloadButtonsRow
import com.chenfa.openclashinstaller.ui.components.EnvCheckRow
import com.chenfa.openclashinstaller.ui.components.GlassSurface
import com.chenfa.openclashinstaller.ui.components.PrimaryButton
import com.chenfa.openclashinstaller.ui.components.PrimaryButtonVariant
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 「安装」页：环境检查 + 下载 + 当前连接摘要 + 全部操作按钮。
 * 内容与旧主屏幕完全一致，VM 回调零改动。
 */
@Composable
fun InstallPage(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onEditConnection: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val tokens = LocalGlassTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 环境检查
        CollapsibleCard(title = "环境检查", defaultExpanded = true) {
            state.envStatus.forEach { EnvCheckRow(it) }
        }

        // 下载
        CollapsibleCard(title = "下载", defaultExpanded = true) {
            DownloadButtonsRow(
                onKernel = vm::downloadKernel,
                onOpenclash = vm::downloadOpenclash,
                enabled = !state.busy,
            )
            Text(
                "·下载走网络；顶栏 ↓ 按钮手动从手机导入 ipk / gz",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onGlassEmphasis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // 当前连接摘要 + 编辑按钮（跳转到「设置」页）
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = GlassShapes.card,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "当前连接",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.onGlass,
                    )
                    Text(
                        "${state.fields.user}@${state.fields.ip}:${state.fields.port}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onGlass,
                    )
                }
                IconButton(onClick = onEditConnection) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "编辑连接",
                        tint = tokens.onGlass,
                    )
                }
            }
        }

        // 操作按钮列表
        PrimaryButton(
            text = "连接测试",
            onClick = vm::testConn,
            enabled = !state.busy,
        )
        PrimaryButton(
            text = "开始安装",
            onClick = vm::install,
            enabled = !state.busy,
        )
        Text(
            "gl 专属功能",
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.sectionLabel,
        )
        PrimaryButton(
            text = "安装风扇控制",
            onClick = vm::installFan,
            enabled = !state.busy,
            variant = PrimaryButtonVariant.TONAL,
            subtitle = "安装完成在GL 管理界面 系统-风扇控制",
        )
        PrimaryButton(
            text = "解锁隐藏功能",
            onClick = vm::unlockHidden,
            enabled = !state.busy,
            variant = PrimaryButtonVariant.TONAL,
            subtitle = "将 GL 管理界面中被 lang_hide 隐藏的菜单项改为中文可见",
        )

        Text(
            "修复 oppo reno5 pro usb 共享网络 bug",
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.sectionLabel,
        )
        PrimaryButton(
            text = "加载修复",
            onClick = vm::fixUsbTethering,
            enabled = !state.busy,
            variant = PrimaryButtonVariant.TONAL,
            subtitle = "内置修复版 RNDIS 内核模块，修复完成手动重启路由器生效",
        )

        Spacer(Modifier.height(20.dp))
    }
}
