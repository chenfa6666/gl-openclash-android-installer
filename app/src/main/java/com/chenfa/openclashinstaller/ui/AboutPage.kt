package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.ui.components.GlassSurface
import com.chenfa.openclashinstaller.ui.theme.GlassShapes
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 「关于」页：应用信息 + 开源致谢。
 *
 * 卡片之间用统一的 spacedBy 间距，避免空隙不一致；致谢卡片列出本项目用到的
 * GitHub 开源项目并致谢，点击可跳转对应仓库。
 */

/** 一个开源项目条目：名称 + 说明 + 仓库地址。 */
private data class Credit(val name: String, val desc: String, val url: String)

private val credits = listOf(
    Credit(
        name = "AndroidLiquidGlass (backdrop)",
        desc = "液态玻璃 / 背景模糊效果原语",
        url = "https://github.com/Kyant0/AndroidLiquidGlass",
    ),
    Credit(
        name = "Jetpack Compose",
        desc = "现代声明式 UI 框架",
        url = "https://github.com/androidx/androidx",
    ),
    Credit(
        name = "JSch (mwiede)",
        desc = "SSH 连接，用于与路由器通信",
        url = "https://github.com/mwiede/jsch",
    ),
    Credit(
        name = "OkHttp",
        desc = "HTTP 客户端，用于下载文件",
        url = "https://github.com/square/okhttp",
    ),
    Credit(
        name = "Kotlin Coroutines",
        desc = "协程，异步任务调度",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    Credit(
        name = "AndroidX DataStore",
        desc = "数据持久化，保存设置",
        url = "https://developer.android.com/topic/libraries/architecture/datastore",
    ),
)

@Composable
fun AboutPage(contentPadding: PaddingValues) {
    val tokens = LocalGlassTokens.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 应用信息
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = GlassShapes.card,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "OpenClash 安装器",
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.onGlass,
                )
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
                Text(
                    "github: https://github.com/chenfa6666/gl-openclash-android-installer",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onGlassEmphasis,
                )
            }
        }

        // 开源致谢
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = GlassShapes.card,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "开源致谢",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.onGlass,
                )
                Text(
                    "本应用基于以下开源项目构建，衷心感谢各项目贡献者：",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onGlassEmphasis,
                )
                credits.forEach { credit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { runCatching { uriHandler.openUri(credit.url) } }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                credit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.onGlass,
                            )
                            Text(
                                credit.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.onGlassEmphasis,
                            )
                        }
                    }
                }
                Text(
                    "感谢以上所有开源项目的贡献者。",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onGlassEmphasis,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
