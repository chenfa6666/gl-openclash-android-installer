package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.LogEntry

/**
 * 日志列表。
 *
 * 稳定 key 让同一进度行原地刷新；[autoScroll] 打开时始终跟随最新一行，
 * 用户想回看历史时可以暂停跟随，不会被新日志打断。
 */
@Composable
fun ProgressLog(
    entries: List<LogEntry>,
    autoScroll: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LaunchedEffect(entries.size, entries.lastOrNull()?.text, autoScroll) {
        if (autoScroll && entries.isNotEmpty()) {
            listState.scrollToItem(entries.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            LogLine(entry = entry)
        }
    }
}
