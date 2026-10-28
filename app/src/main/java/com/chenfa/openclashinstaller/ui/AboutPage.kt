package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.R
import com.chenfa.openclashinstaller.ui.components.BlurredBar
import com.chenfa.openclashinstaller.ui.components.rememberBlurBackdrop
import com.chenfa.openclashinstaller.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private data class Credit(
    val name: String,
    val description: String,
    val url: String,
)

private val credits = listOf(
    Credit(
        name = "KernelSU",
        description = "本次界面的视觉与 Miuix 结构参考",
        url = "https://github.com/tiann/KernelSU",
    ),
    Credit(
        name = "Miuix KMP",
        description = "HyperOS 风格 Compose Multiplatform 组件库",
        url = "https://github.com/compose-miuix-ui/miuix",
    ),
    Credit(
        name = "Jetpack Compose",
        description = "现代声明式 Android UI 框架",
        url = "https://github.com/androidx/androidx",
    ),
    Credit(
        name = "JSch (mwiede)",
        description = "SSH / SCP 连接与命令执行",
        url = "https://github.com/mwiede/jsch",
    ),
    Credit(
        name = "OkHttp",
        description = "安装包与内核下载",
        url = "https://github.com/square/okhttp",
    ),
    Credit(
        name = "Kotlin Coroutines",
        description = "异步任务调度与取消传播",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
    ),
)

@Composable
fun AboutPage(
    bottomPadding: Dp,
    onBack: (() -> Unit)? = null,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val open: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "关于",
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                minWidth = 48.dp,
                                minHeight = 48.dp,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "返回设置",
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = if (backdrop != null) {
                Modifier.layerBackdrop(backdrop)
            } else {
                Modifier
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(colorScheme.primary, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(62.dp),
                            )
                        }
                        Text(
                            text = "OpenClash 安装器",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = "作者 chenfa6666",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        ArrowPreference(
                            title = "项目主页",
                            summary = "github.com/chenfa6666/gl-openclash-android-installer",
                            onClick = { open("https://github.com/chenfa6666/gl-openclash-android-installer") },
                        )
                        ArrowPreference(
                            title = "支持开发",
                            summary = "给项目点一个 Star",
                            onClick = { open("https://github.com/chenfa6666/gl-openclash-android-installer") },
                        )
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        credits.forEach { credit ->
                            ArrowPreference(
                                title = credit.name,
                                summary = credit.description,
                                onClick = { open(credit.url) },
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "使用须知",
                                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                            )
                            Note("请仅在自己拥有或已获授权的路由器上使用本工具。")
                            Note("安装前建议备份路由器配置，安装完成后在「服务 → OpenClash」中启用。")
                            Note("风扇控制与 USB 共享网络修复安装完成后，需要重启路由器生效。")
                            Note("路由器密码仅保存在本机 DataStore，不会上传到任何服务器。")
                            Note("本应用与 GL.iNet、OpenClash 官方无隶属关系，使用风险由使用者自行承担。")
                        }
                    }

                    Spacer(Modifier.height(bottomPadding + 12.dp))
                }
            }
        }
    }
    }
}

@Composable
private fun Note(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "·",
            fontSize = 14.sp,
            color = colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f),
        )
    }
}
