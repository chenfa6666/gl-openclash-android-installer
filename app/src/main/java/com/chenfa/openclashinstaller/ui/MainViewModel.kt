package com.chenfa.openclashinstaller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenfa.openclashinstaller.data.FileChecker
import com.chenfa.openclashinstaller.data.FullLogBuffer
import com.chenfa.openclashinstaller.data.SettingsStore
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.data.model.UiEvent
import com.chenfa.openclashinstaller.data.model.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 主 ViewModel。
 *
 * 阶段 B：仅承载 envStatus / fields / settingsOpen / aboutOpen。
 * 阶段 C+：logEntries / busy / abort 等后续接入。
 */
class MainViewModel(
    private val settingsStore: SettingsStore,
    private val fileChecker: FileChecker,
    private val fullLog: FullLogBuffer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var entryId = 0L
    private fun nextId(): Long = entryId++

    /** 启动：加载持久化设置 + 环境检查。 */
    fun init0() {
        viewModelScope.launch {
            val s = settingsStore.snapshot()
            _uiState.update {
                it.copy(
                    fields = s.fields,
                    kernelUrl = s.kernelUrl,
                    openclashUrl = s.openclashUrl,
                    fanUrl = s.fanUrl,
                )
            }
            refreshEnv()
        }
    }

    /** 刷新环境检查。等价 Windows 版 RefreshEnvCheck。 */
    fun refreshEnv() {
        viewModelScope.launch {
            val items = fileChecker.check()
            val dl = fileChecker.downloadedFiles()
            _uiState.update { it.copy(envStatus = items, downloadedFiles = dl) }
        }
    }

    // ----- 输入字段 -----

    fun setIp(v: String) = update { it.copy(fields = it.fields.copy(ip = v)) }
    fun setUser(v: String) = update { it.copy(fields = it.fields.copy(user = v)) }
    fun setPassword(v: String) = update { it.copy(fields = it.fields.copy(password = v)) }
    fun setPort(v: String) = update { it.copy(fields = it.fields.copy(port = v)) }

    private fun update(block: (UiState) -> UiState) = _uiState.update(block)

    /** 保存输入字段到 DataStore。 */
    fun saveFields() {
        viewModelScope.launch {
            settingsStore.save(settingsStore.snapshot().copy(fields = _uiState.value.fields))
        }
    }

    // ----- 设置对话框 -----

    fun openSettings() = _uiState.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(settingsOpen = false) }

    fun saveSettings(kernelUrl: String, openclashUrl: String, fanUrl: String, fields: ConnFields) {
        viewModelScope.launch {
            val cur = settingsStore.snapshot()
            settingsStore.save(
                cur.copy(
                    kernelUrl = kernelUrl,
                    openclashUrl = openclashUrl,
                    fanUrl = fanUrl,
                    fields = fields,
                )
            )
            _uiState.update {
                it.copy(
                    fields = fields,
                    kernelUrl = kernelUrl,
                    openclashUrl = openclashUrl,
                    fanUrl = fanUrl,
                    settingsOpen = false,
                )
            }
            _events.trySend(UiEvent.Toast("设置已保存"))
        }
    }

    fun openAbout() = _uiState.update { it.copy(aboutOpen = true) }
    fun closeAbout() = _uiState.update { it.copy(aboutOpen = false) }
    fun openConfirmAbort() = _uiState.update { it.copy(confirmAbortOpen = true) }
    fun closeConfirmAbort() = _uiState.update { it.copy(confirmAbortOpen = false) }

    // ----- 日志（阶段 C+ 完整接线，阶段 B 仅占位） -----

    /** 普通日志：完整缓冲 + 界面过滤后追加。 */
    fun appendLog(line: String, kind: LogKind = LogKind.NORMAL) {
        viewModelScope.launch {
            fullLog.append(line)
            val display = com.chenfa.openclashinstaller.data.LogFilter.processLine(line)
            if (display != null) {
                _uiState.update {
                    it.copy(logEntries = it.logEntries + LogEntry(nextId(), display, kind, null))
                }
            }
        }
    }

    /** 进度日志：同 key 覆盖最后一条，原地刷新。 */
    fun appendProgressLog(line: String, key: String = "PROGRESS") {
        viewModelScope.launch {
            fullLog.append(line)
            _uiState.update { s ->
                val list = s.logEntries.toMutableList()
                val idx = list.indexOfLast { it.progressKey == key }
                if (idx >= 0) list[idx] = list[idx].copy(text = line)
                else list += LogEntry(nextId(), line, LogKind.PROGRESS, key)
                s.copy(logEntries = list)
            }
        }
    }
}
