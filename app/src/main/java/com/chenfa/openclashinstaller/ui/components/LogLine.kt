package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LogTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 17.sp,
)

private val TimeStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    lineHeight = 17.sp,
)

private val StepTextStyle = LogTextStyle.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
)

private data class Body(
    val icon: ImageVector?,
    val bullet: Boolean,
    val text: String,
)

private fun bodyOf(entry: LogEntry): Body {
    val raw = entry.text
    val trimmed = raw.trimStart()
    val symbol = trimmed.firstOrNull()
    return when {
        entry.kind == LogKind.SUCCESS -> Body(
            icon = MiuixIcons.Ok,
            bullet = false,
            text = if (symbol == '✓') trimmed.drop(1).trimStart() else trimmed,
        )

        entry.kind == LogKind.ERROR -> Body(
            icon = MiuixIcons.Close2,
            bullet = false,
            text = if (symbol == '✗' || symbol == '×') trimmed.drop(1).trimStart() else trimmed,
        )

        entry.kind == LogKind.WARN -> Body(
            icon = MiuixIcons.Report,
            bullet = false,
            text = if (symbol == '⚠' || symbol == '!') trimmed.drop(1).trimStart() else trimmed,
        )

        symbol == '·' -> Body(
            icon = null,
            bullet = true,
            text = trimmed.drop(1).trimStart(),
        )

        else -> Body(icon = null, bullet = false, text = raw)
    }
}

/**
 * 单行日志：时间 + 级别图标 + 内容。
 *
 * 级别由 ViewModel 根据行首符号自动识别，worker 只管写自然语言；
 * 原始命令输出保留缩进与等宽字体，便于对照路由器返回。
 */
@Composable
fun LogLine(
    entry: LogEntry,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val status = AppStatus
    val color: Color = when (entry.kind) {
        LogKind.SUCCESS -> status.success
        LogKind.ERROR -> scheme.error
        LogKind.WARN -> status.warning
        LogKind.STEP -> scheme.primary
        LogKind.SECTION -> scheme.onSurfaceVariantSummary
        LogKind.PROGRESS -> scheme.primary
        LogKind.NORMAL -> scheme.onSurfaceContainer
    }

    if (entry.kind == LogKind.SECTION) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
        ) {
            HorizontalDivider(color = scheme.dividerLine)
            Text(
                text = entry.text.trim(),
                style = LogTextStyle.copy(fontWeight = FontWeight.Medium),
                color = color,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        return
    }

    val body = bodyOf(entry)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = entry.time,
            style = TimeStyle,
            color = scheme.onSurfaceContainerVariant,
            modifier = Modifier
                .width(52.dp)
                .padding(top = 1.dp),
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .padding(top = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                body.icon != null -> Icon(
                    imageVector = body.icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp),
                )

                entry.kind == LogKind.STEP -> Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(12.dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )

                body.bullet -> Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(color.copy(alpha = 0.65f), CircleShape),
                )
            }
        }
        Text(
            text = body.text,
            style = if (entry.kind == LogKind.STEP) StepTextStyle else LogTextStyle,
            color = color,
            fontWeight = if (entry.kind == LogKind.PROGRESS) FontWeight.Medium else null,
            modifier = Modifier.weight(1f),
        )
    }
}
