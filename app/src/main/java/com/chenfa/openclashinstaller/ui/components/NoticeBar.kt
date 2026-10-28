package com.chenfa.openclashinstaller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.NoticeLevel
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 一条待展示的底部提示。 */
data class NoticeUi(
    val id: Long,
    val message: String,
    val level: NoticeLevel,
    val durationMs: Long,
)

/** 不同级别提示的停留时长：错误留久一点，方便读完。 */
fun NoticeLevel.durationMs(): Long = when (this) {
    NoticeLevel.ERROR -> 6500L
    NoticeLevel.WARNING -> 5200L
    NoticeLevel.SUCCESS, NoticeLevel.INFO -> 3600L
}

private fun NoticeLevel.icon() = when (this) {
    NoticeLevel.SUCCESS -> MiuixIcons.Ok
    NoticeLevel.ERROR -> MiuixIcons.Close2
    NoticeLevel.WARNING -> MiuixIcons.Report
    NoticeLevel.INFO -> MiuixIcons.Info
}

@Composable
private fun accentColor(level: NoticeLevel): Color = when (level) {
    NoticeLevel.SUCCESS -> AppStatus.success
    NoticeLevel.ERROR -> MiuixTheme.colorScheme.error
    NoticeLevel.WARNING -> AppStatus.warning
    NoticeLevel.INFO -> MiuixTheme.colorScheme.primary
}

/**
 * 底部提示条宿主。
 *
 * 放在 Miuix Scaffold 的 snackbarHost 槽位，自动避让底部导航栏；
 * 每次只展示最新一条，同级提示覆盖显示，不堆叠。
 */
@Composable
fun NoticeHost(
    notice: NoticeUi?,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = notice != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        ) {
            if (notice != null) {
                NoticeBar(notice = notice, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun NoticeBar(
    notice: NoticeUi,
    onDismiss: () -> Unit,
) {
    val accent = accentColor(notice.level)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MiuixTheme.colorScheme.surfaceContainerHigh,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = notice.level.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = notice.message,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onDismiss,
                minWidth = 34.dp,
                minHeight = 34.dp,
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = "关闭提示",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
