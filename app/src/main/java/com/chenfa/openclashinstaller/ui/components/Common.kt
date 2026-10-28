package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 状态语义色。 */
enum class StatusTone { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

@Immutable
private data class ToneColors(val container: Color, val content: Color)

@Composable
private fun toneColors(tone: StatusTone): ToneColors {
    val scheme = MiuixTheme.colorScheme
    val status = AppStatus
    return when (tone) {
        StatusTone.SUCCESS -> ToneColors(status.successContainer, status.onSuccessContainer)
        StatusTone.WARNING -> ToneColors(status.warningContainer, status.onWarningContainer)
        StatusTone.ERROR -> ToneColors(scheme.errorContainer, scheme.onErrorContainer)
        StatusTone.INFO -> ToneColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        StatusTone.NEUTRAL -> ToneColors(scheme.surfaceContainerHigh, scheme.onSurfaceContainer)
    }
}

@Composable
private fun toneAccent(tone: StatusTone): Color = when (tone) {
    StatusTone.SUCCESS -> AppStatus.success
    StatusTone.WARNING -> AppStatus.warning
    StatusTone.ERROR -> MiuixTheme.colorScheme.error
    StatusTone.INFO -> MiuixTheme.colorScheme.primary
    StatusTone.NEUTRAL -> MiuixTheme.colorScheme.onSurfaceContainerVariant
}

/**
 * 信息横幅：KernelSU 风格的圆角提示卡，用于需要用户处理但不阻塞操作的提示。
 */
@Composable
fun InfoBanner(
    title: String,
    message: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = toneColors(tone)
    val accent = toneAccent(tone)
    val icon = when (tone) {
        StatusTone.SUCCESS -> MiuixIcons.Ok
        StatusTone.WARNING -> MiuixIcons.Report
        StatusTone.ERROR -> MiuixIcons.Close2
        StatusTone.INFO, StatusTone.NEUTRAL -> MiuixIcons.Info
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.headline2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(
                    text = actionLabel,
                    onClick = onAction,
                    minWidth = 44.dp,
                    minHeight = 32.dp,
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    textStyle = MiuixTheme.textStyles.footnote1,
                    colors = ButtonDefaults.textButtonColors(
                        color = Color.Transparent,
                        disabledColor = Color.Transparent,
                        textColor = colors.content,
                        disabledTextColor = colors.content.copy(alpha = 0.4f),
                    ),
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, minWidth = 34.dp, minHeight = 34.dp) {
                    Icon(
                        imageVector = MiuixIcons.Close,
                        contentDescription = "关闭提示",
                        tint = colors.content.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
