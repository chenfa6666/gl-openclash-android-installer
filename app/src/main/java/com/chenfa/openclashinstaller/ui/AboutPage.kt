package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.ui.components.GlassSurface
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 「关于」页：应用名 + 作者 + 版本号 + 仓库地址。内容与旧关于对话框一致。
 */
@Composable
fun AboutPage(contentPadding: PaddingValues) {
    val tokens = LocalGlassTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = GlassShapes.card,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "OpenClash 安装器",
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.onGlass,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.onGlass,
                )
                Text(
                    "作者: chenfa6666",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.onGlass,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "github: https://github.com/chenfa6666/gl-openclash-android-installer",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onGlassEmphasis,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
