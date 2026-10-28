package com.chenfa.openclashinstaller.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chenfa.openclashinstaller.core.ext.formatDuration
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.data.model.OpId
import com.chenfa.openclashinstaller.data.model.OpOutcome
import com.chenfa.openclashinstaller.data.model.OpStatus
import com.chenfa.openclashinstaller.ui.theme.AppStatus
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全屏任务日志弹窗。
 *
 * 结构：状态卡（进度 / 耗时 / 当前步骤）+ 日志工具栏 + 可滚动日志 + 底部操作。
 * 任务结束后保留日志，支持分享完整日志、失败重试、重新打开查看。
 */
@Composable
fun OperationDialog(
    title: String,
    entries: List<LogEntry>,
    busy: Boolean,
    aborting: Boolean,
    confirmAbortOpen: Boolean,
    startedAt: Long?,
    outcome: OpOutcome?,
    onClose: () -> Unit,
    onAbortRequest: () -> Unit,
    onConfirmAbort: () -> Unit,
    onDismissAbort: () -> Unit,
    onRetry: (OpId) -> Unit,
    onShareLog: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onClose() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(enabled = !busy) { onClose() }

        var showProblemsOnly by rememberSaveable { mutableStateOf(false) }
        var autoScroll by rememberSaveable { mutableStateOf(true) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

        LaunchedEffect(busy, startedAt) {
            if (busy) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1000L)
                }
            } else {
                now = System.currentTimeMillis()
            }
        }

        val elapsed = startedAt?.let { start ->
            val end = outcome?.finishedAt ?: now
            (end - start).coerceAtLeast(0L)
        } ?: 0L

        val percent = entries.lastOrNull()
            ?.takeIf { it.kind == LogKind.PROGRESS }
            ?.progressPercent
        val currentStep = entries.lastOrNull { it.kind == LogKind.STEP }?.text?.trim()
        val problemCount = entries.count {
            it.kind == LogKind.ERROR || it.kind == LogKind.WARN
        }
        val visibleEntries = remember(entries, showProblemsOnly) {
            if (showProblemsOnly) {
                entries.filter { it.kind == LogKind.ERROR || it.kind == LogKind.WARN }
            } else {
                entries
            }
        }

        val statusTitle: String
        val statusDetail: String
        val statusTone: StatusTone
        val statusIcon = when {
            busy -> MiuixIcons.Info
            outcome?.status == OpStatus.SUCCESS -> MiuixIcons.Ok
            outcome?.status == OpStatus.FAILED -> MiuixIcons.Close2
            outcome?.status == OpStatus.CANCELLED -> MiuixIcons.Report
            else -> MiuixIcons.Info
        }
        when {
            busy -> {
                statusTitle = if (aborting) "正在结束任务" else "正在执行"
                statusDetail = currentStep?.takeIf { it.isNotBlank() } ?: "正在准备，请稍候…"
                statusTone = StatusTone.INFO
            }

            outcome?.status == OpStatus.SUCCESS -> {
                statusTitle = "任务完成"
                statusDetail = outcome.summary
                statusTone = StatusTone.SUCCESS
            }

            outcome?.status == OpStatus.FAILED -> {
                statusTitle = "任务未完成"
                statusDetail = outcome.summary
                statusTone = StatusTone.ERROR
            }

            outcome?.status == OpStatus.CANCELLED -> {
                statusTitle = "任务已取消"
                statusDetail = "已中断当前操作，可以关闭日志或重试"
                statusTone = StatusTone.WARNING
            }

            else -> {
                statusTitle = "运行日志"
                statusDetail = "本次任务的完整输出"
                statusTone = StatusTone.NEUTRAL
            }
        }

        val accent = when (statusTone) {
            StatusTone.SUCCESS -> AppStatus.success
            StatusTone.WARNING -> AppStatus.warning
            StatusTone.ERROR -> MiuixTheme.colorScheme.error
            StatusTone.INFO -> MiuixTheme.colorScheme.primary
            StatusTone.NEUTRAL -> MiuixTheme.colorScheme.onSurfaceContainerVariant
        }

        Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MiuixTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onClose,
                        enabled = !busy,
                        minWidth = 48.dp,
                        minHeight = 48.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "关闭日志",
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                busy && aborting -> "正在结束…"
                                busy -> "执行中 · ${formatDuration(elapsed)}"
                                outcome != null -> "耗时 ${formatDuration(elapsed)}"
                                else -> "历史日志"
                            },
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = onShareLog,
                        enabled = entries.isNotEmpty(),
                        minWidth = 48.dp,
                        minHeight = 48.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Share,
                            contentDescription = "分享运行日志",
                            tint = if (entries.isEmpty()) {
                                MiuixTheme.colorScheme.disabledOnSurface
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatusCard(
                        title = statusTitle,
                        detail = statusDetail,
                        icon = statusIcon,
                        accent = accent,
                        elapsedMs = elapsed,
                        busy = busy,
                        aborting = aborting,
                        percent = percent,
                        completedSuccess = outcome?.status == OpStatus.SUCCESS,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "运行日志",
                            style = MiuixTheme.textStyles.headline2,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                append("${entries.size} 条")
                                if (problemCount > 0) append(" · $problemCount 个问题")
                            },
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(Modifier.weight(1f))
                        LogToolbarIcon(
                            active = showProblemsOnly,
                            onClick = { showProblemsOnly = !showProblemsOnly },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Filter,
                                contentDescription = if (showProblemsOnly) "显示全部日志" else "只看问题",
                                tint = it,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        LogToolbarIcon(
                            active = autoScroll,
                            onClick = { autoScroll = !autoScroll },
                        ) {
                            Icon(
                                imageVector = if (autoScroll) MiuixIcons.Pause else MiuixIcons.Play,
                                contentDescription = if (autoScroll) "暂停跟随日志" else "恢复跟随日志",
                                tint = it,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MiuixTheme.colorScheme.surfaceContainer,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                        border = BorderStroke(0.75.dp, MiuixTheme.colorScheme.dividerLine),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (visibleEntries.isEmpty()) {
                                EmptyLogState(showProblemsOnly = showProblemsOnly)
                            } else {
                                ProgressLog(
                                    entries = visibleEntries,
                                    autoScroll = autoScroll,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    BottomActions(
                        busy = busy,
                        aborting = aborting,
                        outcome = outcome,
                        onClose = onClose,
                        onAbortRequest = onAbortRequest,
                        onRetry = onRetry,
                    )
                }
            }
        }
            if (confirmAbortOpen) {
                AbortConfirmOverlay(
                    onCancel = onDismissAbort,
                    onConfirm = onConfirmAbort,
                )
            }
        }
    }
}

@Composable
private fun AbortConfirmOverlay(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "强制结束任务？",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = "任务会被立即中断，未完成的文件写入可能不完整。",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        minHeight = 42.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("强制结束", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    elapsedMs: Long,
    busy: Boolean,
    aborting: Boolean,
    percent: Int?,
    completedSuccess: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.headline2,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    aborting -> "结束中"
                    busy && percent != null -> "$percent%"
                    busy -> "…"
                    else -> formatDuration(elapsedMs)
                },
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Medium,
                color = accent,
            )
        }

        if (busy || completedSuccess) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = when {
                    busy -> percent?.let { it / 100f }
                    else -> 1f
                },
                height = 6.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = accent,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun LogToolbarIcon(
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val tint = if (active) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    IconButton(
        onClick = onClick,
        minWidth = 38.dp,
        minHeight = 38.dp,
    ) {
        content(tint)
    }
}

@Composable
private fun EmptyLogState(showProblemsOnly: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MiuixIcons.Info,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (showProblemsOnly) "当前没有警告或错误" else "等待任务输出…",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

@Composable
private fun BottomActions(
    busy: Boolean,
    aborting: Boolean,
    outcome: OpOutcome?,
    onClose: () -> Unit,
    onAbortRequest: () -> Unit,
    onRetry: (OpId) -> Unit,
) {
    if (busy) {
        Button(
            onClick = onAbortRequest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !aborting,
            minHeight = 46.dp,
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.error,
                disabledColor = MiuixTheme.colorScheme.disabledSecondary,
                contentColor = MiuixTheme.colorScheme.onError,
                disabledContentColor = MiuixTheme.colorScheme.disabledOnSecondary,
            ),
        ) {
            Text(if (aborting) "正在结束…" else "强制结束任务")
        }
        return
    }

    val canRetry = outcome?.status == OpStatus.FAILED || outcome?.status == OpStatus.CANCELLED
    if (canRetry) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = "关闭",
                onClick = onClose,
                minWidth = 74.dp,
                minHeight = 46.dp,
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                colors = ButtonDefaults.textButtonColors(),
            )
            Button(
                onClick = { onRetry(outcome.op) },
                modifier = Modifier.weight(1f),
                minHeight = 46.dp,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("重试")
            }
        }
    } else {
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 46.dp,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(if (outcome?.status == OpStatus.SUCCESS) "完成" else "关闭")
        }
    }
}
