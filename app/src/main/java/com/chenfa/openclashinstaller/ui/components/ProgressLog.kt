package com.chenfa.openclashinstaller.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.LogEntry

/**
 * 右侧日志列表：LazyColumn + items(key=id) + animateContentSize。
 *
 * 进度单行原地刷新机制（等价 Windows 版 EM_SETSEL/EM_REPLACESEL）：
 * 每条 LogEntry 有稳定 id，Compose items(key=id) 保证同一 id 复用同一 Composable；
 * ViewModel 用同 progressKey 覆盖最后一条时，id 不变 → Compose 直接对原 Composable 调 setText，
 * animateContentSize 让 text 变化时不闪屏，达到原地刷新效果。
 *
 * 自动滚到底部：新增条目时滚动到末尾（等价 Windows 版 EM_SCROLLCARET）。
 */
@Composable
fun ProgressLog(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()

    // 自动滚到底部：当 entries 数量变化或最后一条 text 变化时
    LaunchedEffect(entries.size, entries.lastOrNull()?.text) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(8.dp),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            LogLine(
                text = entry.text,
                kind = entry.kind,
                isDark = isDark,
                modifier = Modifier.animateContentSize(),
            )
        }
    }
}
