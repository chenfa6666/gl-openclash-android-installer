package com.chenfa.openclashinstaller.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
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
import com.chenfa.openclashinstaller.domain.WorkerFan
import com.chenfa.openclashinstaller.domain.WorkerInstall
import com.chenfa.openclashinstaller.domain.WorkerTestConn
import com.chenfa.openclashinstaller.domain.WorkerUnlockHidden
import com.chenfa.openclashinstaller.domain.WorkerUsbTethering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 主 ViewModel。
 *
 * 阶段 C：新增 SshClient/Downloader/appConfig 注入；downloadKernel/downloadOpenclash/testConn/abort 接线；
 * 日志双轨制（完整缓冲 + 界面过滤）+ 进度单行原地刷新；currentJob 支持取消传播。
 */
class MainViewModel(
    private val appContext: Context,
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
        viewModelScope.launch { refreshEnvSync() }
    }

    /** 同步刷新（在已有 suspend 协程里调用，避免起新 job 异步不保证顺序）。 */
    private suspend fun refreshEnvSync() {
        val items = fileChecker.check()
        val dl = fileChecker.downloadedFiles()
        _uiState.update { it.copy(envStatus = items, downloadedFiles = dl) }
    }

    // ----- 操作编排（launchOp + 取消传播） -----

    private fun launchOp(op: OpId, block: suspend () -> Unit) {
        if (_uiState.value.busy) {
            _events.trySend(UiEvent.Toast("当前正忙，无法启动新操作"))
            return
        }
        _uiState.update {
            it.copy(
                busy = true,
                activeOp = op,
                operationOpen = true,
                operationTitle = opTitle(op),
                logEntries = emptyList(),  // 清空界面日志：只显示本次操作
                // 完整日志 fullLog 不清空（导出能查到所有历史）
            )
        }
        currentJob = viewModelScope.launch {
            try {
                block()
            } catch (c: CancellationException) {
                appendLog("! 操作已取消", LogKind.WARN)
                throw c
            } catch (e: Throwable) {
                appendLog("✗ 操作异常: ${e.message}", LogKind.ERROR)
            } finally {
                _uiState.update { it.copy(busy = false, activeOp = null) }
                refreshEnvSync()
            }
        }
    }

    private fun opTitle(op: OpId): String = when (op) {
        OpId.DL_KERNEL -> "下载内核"
        OpId.DL_OPENCLASH -> "下载 OpenClash ipk"
        OpId.TESTCONN -> "连接测试"
        OpId.INSTALL -> "开始安装"
        OpId.FAN -> "安装风扇控制"
        OpId.UNLOCK_HIDDEN -> "解锁隐藏功能"
        OpId.USB_TETHERING -> "USB 共享网络修复"
    }

    /** 关闭操作弹窗（busy 时拒绝）。 */
    fun closeOperation() {
        if (_uiState.value.busy) {
            _events.trySend(UiEvent.Toast("操作进行中，请先强制结束再关闭"))
            return
        }
        _uiState.update { it.copy(operationOpen = false) }
    }

    fun downloadKernel() = launchOp(OpId.DL_KERNEL) {
        appendLog("--- 开始下载内核 ---", LogKind.NORMAL)
        val worker = WorkerDownload(
            downloader = downloader,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        val ok = worker.execute(
            _uiState.value.kernelUrl,
            appConfig.localKernel.absolutePath,
            "内核",
        )
        appendLog(if (ok) "✓ 内核下载完成" else "✗ 内核下载失败", if (ok) LogKind.SUCCESS else LogKind.ERROR)
        refreshEnvSync()
    }

    fun downloadOpenclash() = launchOp(OpId.DL_OPENCLASH) {
        appendLog("--- 开始下载 OpenClash ipk ---", LogKind.NORMAL)
        val worker = WorkerDownload(
            downloader = downloader,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        val ok = worker.execute(
            _uiState.value.openclashUrl,
            appConfig.localIpk.absolutePath,
            "OpenClash ipk",
        )
        appendLog(if (ok) "✓ OpenClash ipk 下载完成" else "✗ OpenClash ipk 下载失败", if (ok) LogKind.SUCCESS else LogKind.ERROR)
        refreshEnvSync()
    }

    fun testConn() = launchOp(OpId.TESTCONN) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val worker = WorkerTestConn(ssh = ssh, onLog = { appendLog(it) })
        worker.execute(
            s.fields.ip,
            s.fields.user,
            s.fields.port.toIntOrNull() ?: 22,
            s.fields.password,
        )
    }

    /** 4 步安装 OpenClash（依赖 → SCP → opkg → 内核）。 */
    fun install() = launchOp(OpId.INSTALL) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val r = WorkerInstall.execute(
            ssh = ssh,
            downloader = downloader,
            appConfig = appConfig,
            fields = s.fields,
            urls = Triple(s.kernelUrl, s.openclashUrl, s.fanUrl),
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        if (r.getOrNull() == true) {
            appendLog("✓ 安装完成", LogKind.SUCCESS)
        } else if (!r.isSuccess) {
            appendLog("✗ 安装流程异常结束", LogKind.ERROR)
        }
    }

    /** 3 步安装风扇控制（GL.iNet 专属）。 */
    fun installFan() = launchOp(OpId.FAN) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val r = WorkerFan.execute(
            ssh = ssh,
            downloader = downloader,
            appConfig = appConfig,
            fields = s.fields,
            fanUrl = s.fanUrl,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        if (r.getOrNull() == true) {
            appendLog("✓ 风扇控制安装完成", LogKind.SUCCESS)
        } else if (!r.isSuccess) {
            appendLog("✗ 风扇控制安装流程异常结束", LogKind.ERROR)
        }
    }

    /** 解锁 GL.iNet 管理界面隐藏菜单项（替换 lang_hide 中的 zh-cn → zh-tw）。 */
    fun unlockHidden() = launchOp(OpId.UNLOCK_HIDDEN) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val r = WorkerUnlockHidden.execute(
            ssh = ssh,
            fields = s.fields,
            onLog = { appendLog(it) },
        )
        if (r.getOrNull() == true) {
            appendLog("✓ 解锁隐藏功能完成", LogKind.SUCCESS)
        } else if (!r.isSuccess) {
            appendLog("✗ 解锁流程异常结束", LogKind.ERROR)
        }
    }

    /** 修复 OPPO Reno5 Pro USB 共享网络 bug（内置 ipk SCP 上传 + opkg install --force-reinstall）。 */
    fun fixUsbTethering() = launchOp(OpId.USB_TETHERING) {
        val s = _uiState.value
        if (!s.fields.isComplete()) {
            appendLog("✗ 请先在设置中填写 用户名 / IP / 密码 / 端口", LogKind.ERROR)
            return@launchOp
        }
        val r = WorkerUsbTethering.execute(
            ssh = ssh,
            fields = s.fields,
            appContext = appContext,
            filesDir = appConfig.rootDir,
            onLog = { appendLog(it) },
        )
        if (r.getOrNull() == true) {
            appendLog("✓ USB 共享网络修复完成", LogKind.SUCCESS)
        } else if (!r.isSuccess) {
            appendLog("✗ USB 共享网络修复异常结束", LogKind.ERROR)
        }
    }

    /**
     * 从手机本地导入 ipk / gz 文件到 app filesDir（顶栏导入按钮回调）。
     *
     * 规则：
     *  · 文件名以 .tar.gz 结尾或 clash-linux-*.tar.gz 约定 → 作为内核，重命名为 KERNEL_FILE 默认名（若冲突覆盖）
     *  · 文件名匹配 luci-app-openclash_*_all.ipk → 作为 openclash ipk，保留原名
     *  · 文件名精确 == Constants.FAN_IPK_FILE → 作为风扇 ipk
     *  · 其它 .ipk / .gz 也导入（只要扩展名对；list-installed 不校验文件名，内容对即可）
     */
    fun importLocalFile(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val cr = appContext.contentResolver
                    val displayName = queryDisplayName(cr, uri)
                        ?: uri.lastPathSegment?.substringAfterLast('/')
                        ?: throw IllegalArgumentException("无法获取文件名")
                    val name = sanitizeFilename(displayName)
                    val destName = canonicalizeDestName(name)
                    val dest = File(appConfig.rootDir, destName)
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    cr.openInputStream(uri).use { input ->
                        requireNotNull(input) { "内容提供者打开失败" }
                        FileOutputStream(dest).use { out ->
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                out.write(buf, 0, n)
                                total += n
                            }
                            out.flush()
                        }
                    }
                    Triple(dest.name, total, recognizeKind(dest.name))
                }.onSuccess { (name, bytes, kind) ->
                    _events.trySend(
                        UiEvent.Toast(
                            "已导入 $name （${formatSizeSimple(bytes)}） → $kind"
                        )
                    )
                }.onFailure { e ->
                    _events.trySend(UiEvent.Toast("导入失败：${e.message ?: e.javaClass.simpleName}"))
                }
            }
            refreshEnvSync()
        }
    }

    /** 从 content resolver 拿原始文件名（不用 _displayName）。失败返回 null。 */
    private fun queryDisplayName(cr: android.content.ContentResolver, uri: Uri): String? =
        runCatching {
            val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
            cr.query(uri, proj, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()

    private fun sanitizeFilename(raw: String): String =
        raw.replace('/', '_').replace('\u0000', '_').trim('_')

    /** 归类：内核 / openclash ipk / 风扇 ipk / 未知 ipk / 未知 gz。 */
    private fun canonicalizeDestName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".tar.gz") -> {
                // 如果文件名明确是 clash 内核（不是 openclash ipk 的 gz）→ 重命名为 KERNEL_FILE 让环境检查直接 YES
                if (name.startsWith("clash-linux-")) Constants.KERNEL_FILE else name
            }
            lower == Constants.FAN_IPK_FILE.lowercase() -> Constants.FAN_IPK_FILE
            lower.matches(Regex("luci-app-openclash_.+_all\\.ipk")) -> name
            else -> name
        }
    }

    private fun recognizeKind(name: String): String {
        val lower = name.lowercase()
        return when {
            name == Constants.KERNEL_FILE || name.startsWith("clash-linux-") && name.endsWith(".tar.gz") -> "内核"
            lower.matches(Regex("luci-app-openclash_.+_all\\.ipk")) -> "OpenClash ipk"
            lower == Constants.FAN_IPK_FILE.lowercase() -> "风扇控制 ipk"
            lower.endsWith(".ipk") -> "ipk（未识别类型）"
            lower.endsWith(".gz") -> "压缩包（未识别）"
            else -> "其它文件"
        }
    }

    private fun formatSizeSimple(b: Long): String = when {
        b >= 1024 * 1024 -> "%.2f MB".format(b / 1024.0 / 1024.0)
        b >= 1024 -> "%.2f KB".format(b / 1024.0)
        else -> "$b B"
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

    /** 触发一个 Toast 消息（通过 events Channel 投递，UI 收集后用 Snackbar 显示）。 */
    fun toast(msg: String) = _events.trySend(UiEvent.Toast(msg))

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
