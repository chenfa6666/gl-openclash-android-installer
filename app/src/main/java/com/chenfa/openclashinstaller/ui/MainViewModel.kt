package com.chenfa.openclashinstaller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.data.FileChecker
import com.chenfa.openclashinstaller.data.FullLogBuffer
import com.chenfa.openclashinstaller.data.LogFilter
import com.chenfa.openclashinstaller.data.SettingsStore
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.data.model.OpId
import com.chenfa.openclashinstaller.data.model.UiEvent
import com.chenfa.openclashinstaller.data.model.UiState
import com.chenfa.openclashinstaller.data.repo.Downloader
import com.chenfa.openclashinstaller.data.repo.SshClient
import com.chenfa.openclashinstaller.domain.WorkerDownload
import com.chenfa.openclashinstaller.domain.WorkerTestConn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
 * 阶段 C：新增 SshClient/Downloader/appConfig 注入；downloadKernel/downloadOpenclash/testConn/abort 接线；
 * 日志双轨制（完整缓冲 + 界面过滤）+ 进度单行原地刷新；currentJob 支持取消传播。
 */
class MainViewModel(
    private val settingsStore: SettingsStore,
    private val fileChecker: FileChecker,
    private val fullLog: FullLogBuffer,
    private val ssh: SshClient,
    private val downloader: Downloader,
    private val appConfig: AppConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var entryId = 0L
    private var currentJob: Job? = null
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

    // ----- 操作编排（launchOp + 取消传播） -----

    private fun launchOp(op: OpId, block: suspend () -> Unit) {
        if (_uiState.value.busy) {
            _events.trySend(UiEvent.Toast("当前正忙，无法启动新操作"))
            return
        }
        currentJob = viewModelScope.launch {
            _uiState.update { it.copy(busy = true, activeOp = op) }
            try {
                block()
            } catch (c: CancellationException) {
                appendLog("! 操作已取消", LogKind.WARN)
                throw c
            } catch (e: Throwable) {
                appendLog("✗ 操作异常: ${e.message}", LogKind.ERROR)
            } finally {
                _uiState.update { it.copy(busy = false, activeOp = null) }
                refreshEnv()
            }
        }
    }

    fun downloadKernel() = launchOp(OpId.DL_KERNEL) {
        appendLog("--- 开始下载内核 ---", LogKind.NORMAL)
        val worker = WorkerDownload(downloader, ::appendLog, ::appendProgressLog)
        worker.execute(
            _uiState.value.kernelUrl,
            appConfig.localKernel.absolutePath,
            "内核",
        )
    }

    fun downloadOpenclash() = launchOp(OpId.DL_OPENCLASH) {
        appendLog("--- 开始下载 OpenClash ipk ---", LogKind.NORMAL)
        val worker = WorkerDownload(downloader, ::appendLog, ::appendProgressLog)
        worker.execute(
            _uiState.value.openclashUrl,
            appConfig.localIpk.absolutePath,
            "OpenClash ipk",
        )
    }

    fun testConn() = launchOp(OpId.TESTCONN) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val worker = WorkerTestConn(ssh, ::appendLog)
        worker.execute(
            s.fields.ip,
            s.fields.user,
            s.fields.port.toIntOrNull() ?: 22,
            s.fields.password,
        )
    }

    fun abort() {
        if (!_uiState.value.busy) {
            _events.trySend(UiEvent.Toast("当前无运行中的操作"))
            return
        }
        currentJob?.cancel()
        ssh.cancel()  // 等价 Windows 版 TerminateProcess + WinHttpCloseHandle
        _uiState.update { it.copy(busy = false, activeOp = null) }
        viewModelScope.launch {
            appendLog("! 已强制结束当前操作", LogKind.WARN)
            refreshEnv()
        }
    }

    // ----- 输入字段（设置对话框编辑，主屏幕只显示摘要） -----

    fun setIp(v: String) = _uiState.update { it.copy(fields = it.fields.copy(ip = v)) }
    fun setUser(v: String) = _uiState.update { it.copy(fields = it.fields.copy(user = v)) }
    fun setPassword(v: String) = _uiState.update { it.copy(fields = it.fields.copy(password = v)) }
    fun setPort(v: String) = _uiState.update { it.copy(fields = it.fields.copy(port = v)) }

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

    // ----- 日志双轨制（完整缓冲 + 界面过滤 + 进度原地刷新） -----

    /** 普通日志：完整缓冲 + 界面过滤后追加。 */
    suspend fun appendLog(line: String, kind: LogKind = LogKind.NORMAL) {
        fullLog.append(line)
        val display = LogFilter.processLine(line)
        if (display != null) {
            _uiState.update {
                it.copy(logEntries = it.logEntries + LogEntry(nextId(), display, kind, null))
            }
        }
    }

    /** 成功行：✓ 绿。 */
    suspend fun appendSuccess(line: String) = appendLog(line, LogKind.SUCCESS)

    /** 错误行：✗ 红。 */
    suspend fun appendError(line: String) = appendLog(line, LogKind.ERROR)

    /** 进度日志：同 key 覆盖最后一条，原地刷新。 */
    suspend fun appendProgressLog(line: String, key: String = "PROGRESS") {
        fullLog.append(line)
        _uiState.update { s ->
            val list = s.logEntries.toMutableList()
            val idx = list.indexOfLast { it.progressKey == key }
            if (idx >= 0) list[idx] = list[idx].copy(text = line)
            else list += LogEntry(nextId(), line, LogKind.PROGRESS, key)
            s.copy(logEntries = list)
        }
    }

    /** 完整日志快照（导出用）。 */
    suspend fun snapshotFullLog(): String = fullLog.snapshot()

    override fun onCleared() {
        super.onCleared()
        ssh.disconnect()
    }
}
