package com.chenfa.openclashinstaller.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenfa.openclashinstaller.BuildConfig
import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.core.ext.formatBytes
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.FileChecker
import com.chenfa.openclashinstaller.data.FullLogBuffer
import com.chenfa.openclashinstaller.data.LogFilter
import com.chenfa.openclashinstaller.data.SettingsStore
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.LogEntry
import com.chenfa.openclashinstaller.data.model.LogKind
import com.chenfa.openclashinstaller.data.model.NoticeLevel
import com.chenfa.openclashinstaller.data.model.OpId
import com.chenfa.openclashinstaller.data.model.OpOutcome
import com.chenfa.openclashinstaller.data.model.OpStatus
import com.chenfa.openclashinstaller.data.model.Settings
import com.chenfa.openclashinstaller.data.model.ThemeMode
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 单次任务的内部结果，用于生成操作结束卡片。 */
private sealed interface OpResult {
    val success: Boolean
    val summary: String

    data class Success(override val summary: String) : OpResult {
        override val success: Boolean = true
    }

    data class Failed(override val summary: String) : OpResult {
        override val success: Boolean = false
    }

    data class Cancelled(override val summary: String) : OpResult {
        override val success: Boolean = false
    }
}

/**
 * 主 ViewModel。
 *
 * UI 只消费 [UiState] 与一次性 [UiEvent]，业务校验、日志整理、任务结果都收在这里：
 *  - 统一的操作编排：忙碌保护、连接校验、异常翻译、结果快照、取消传播
 *  - 日志双轨制：界面按级别过滤 / 着色，完整日志用于分享
 *  - 设置保存与本地文件导入的错误提示统一走 [notify]
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

    private val entryId = java.util.concurrent.atomic.AtomicLong(0L)
    private var currentJob: Job? = null

    private fun nextId(): Long = entryId.getAndIncrement()

    /** 启动：加载持久化设置 + 环境检查。 */
    fun init0() {
        viewModelScope.launch {
            runCatching { settingsStore.snapshot() }
                .onSuccess { settings ->
                    _uiState.update {
                        it.copy(
                            fields = settings.fields,
                            kernelUrl = settings.kernelUrl,
                            openclashUrl = settings.openclashUrl,
                            fanUrl = settings.fanUrl,
                            themeMode = settings.themeMode,
                            monetEnabled = settings.monetEnabled,
                            blurEnabled = settings.blurEnabled,
                            floatingBottomBar = settings.floatingBottomBar,
                            glassEnabled = settings.glassEnabled,
                            pageSwitchGesture = settings.pageSwitchGesture,
                            uiScale = settings.uiScale,
                        )
                    }
                }
                .onFailure {
                    notify("读取本地设置失败，将使用默认值", NoticeLevel.WARNING)
                }
            refreshEnvSync()
        }
    }

    // ----- 环境检查 -----

    /** 手动刷新环境检查（安装页刷新按钮）。 */
    fun refreshEnv() {
        viewModelScope.launch {
            refreshEnvSync()
            notify("环境检查已刷新", NoticeLevel.SUCCESS)
        }
    }

    private suspend fun refreshEnvSync() {
        try {
            val items = fileChecker.check()
            val files = fileChecker.downloadedFiles()
            _uiState.update { it.copy(envStatus = items, downloadedFiles = files) }
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            notify("环境检查失败：${e.toUserMessage("请稍后重试")}", NoticeLevel.WARNING)
        }
    }

    // ----- 操作编排 -----

    private fun launchOp(
        op: OpId,
        requiresConnection: Boolean = false,
        precheck: (() -> String?)? = null,
        block: suspend () -> OpResult,
    ) {
        val state = _uiState.value
        if (state.busy) {
            val running = state.operationTitle.ifBlank { opTitle(op) }
            notify("正在执行「$running」，请先结束当前任务", NoticeLevel.WARNING)
            return
        }
        if (requiresConnection) {
            state.fields.firstError()?.let { error ->
                notify("连接信息不完整：$error", NoticeLevel.WARNING)
                return
            }
        }
        precheck?.invoke()?.let { error ->
            notify(error, NoticeLevel.WARNING)
            return
        }

        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                busy = true,
                aborting = false,
                activeOp = op,
                operationOpen = true,
                operationTitle = opTitle(op),
                operationStartedAt = startedAt,
                outcome = null,
                logEntries = emptyList(),
                confirmAbortOpen = false,
            )
        }

        currentJob = viewModelScope.launch {
            var result: OpResult = OpResult.Failed("操作未完成")
            var cancelled = false
            try {
                result = block()
            } catch (c: CancellationException) {
                cancelled = true
                result = OpResult.Cancelled("操作已取消")
                appendLogNow("! 操作已取消", LogKind.WARN)
                throw c
            } catch (e: Throwable) {
                if (!currentCoroutineContext().isActive) {
                    cancelled = true
                    result = OpResult.Cancelled("操作已取消")
                    appendLogNow("! 操作已取消", LogKind.WARN)
                    return@launch
                }
                val message = e.toUserMessage()
                result = OpResult.Failed(message)
                appendLog("操作异常：$message", LogKind.ERROR)
            } finally {
                val finishedAt = System.currentTimeMillis()
                val status = when {
                    cancelled || !currentCoroutineContext().isActive -> OpStatus.CANCELLED
                    result.success -> OpStatus.SUCCESS
                    else -> OpStatus.FAILED
                }
                _uiState.update {
                    it.copy(
                        busy = false,
                        aborting = false,
                        activeOp = null,
                        outcome = OpOutcome(
                            op = op,
                            title = opTitle(op),
                            status = status,
                            summary = if (status == OpStatus.CANCELLED) "任务已取消" else result.summary,
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                        ),
                    )
                }
                withContext(NonCancellable) { refreshEnvSync() }
            }
        }
    }

    private fun opTitle(op: OpId): String = when (op) {
        OpId.DL_KERNEL -> "下载 Clash 内核"
        OpId.DL_OPENCLASH -> "下载 OpenClash 插件"
        OpId.DL_FAN -> "下载风扇控制插件"
        OpId.TESTCONN -> "路由器连接测试"
        OpId.INSTALL -> "安装 OpenClash"
        OpId.FAN -> "安装风扇控制"
        OpId.UNLOCK_HIDDEN -> "解锁隐藏菜单"
        OpId.USB_TETHERING -> "修复 USB 共享网络"
    }

    fun downloadKernel() = launchOp(
        op = OpId.DL_KERNEL,
        precheck = { Settings.urlError(_uiState.value.kernelUrl, "内核") },
    ) {
        val url = _uiState.value.kernelUrl.trim()
        appendLog("开始下载 Clash 内核", LogKind.NORMAL)
        val ok = WorkerDownload(
            downloader = downloader,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        ).execute(url, appConfig.localKernel.absolutePath, "Clash 内核")
        if (ok) {
            val size = appConfig.localKernel.takeIf { it.isFile }?.length() ?: 0L
            OpResult.Success("Clash 内核已就绪（${formatBytes(size)}），安装时可自动使用")
        } else {
            OpResult.Failed("内核下载失败，请检查网络或下载地址后重试")
        }
    }

    fun downloadOpenclash() = launchOp(
        op = OpId.DL_OPENCLASH,
        precheck = { Settings.urlError(_uiState.value.openclashUrl, "OpenClash") },
    ) {
        val url = _uiState.value.openclashUrl.trim()
        appendLog("开始下载 OpenClash 插件", LogKind.NORMAL)
        val ok = WorkerDownload(
            downloader = downloader,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        ).execute(url, appConfig.localIpk.absolutePath, "OpenClash 插件")
        if (ok) {
            val size = appConfig.localIpk.takeIf { it.isFile }?.length() ?: 0L
            OpResult.Success("OpenClash 插件已就绪（${formatBytes(size)}），安装时可自动使用")
        } else {
            OpResult.Failed("插件下载失败，请检查网络或下载地址后重试")
        }
    }

    fun downloadFan() = launchOp(
        op = OpId.DL_FAN,
        precheck = { Settings.urlError(_uiState.value.fanUrl, "风扇控制") },
    ) {
        val url = _uiState.value.fanUrl.trim()
        appendLog("开始下载风扇控制插件", LogKind.NORMAL)
        val ok = WorkerDownload(
            downloader = downloader,
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        ).execute(url, appConfig.localFan.absolutePath, "风扇控制插件")
        if (ok) {
            val size = appConfig.localFan.takeIf { it.isFile }?.length() ?: 0L
            OpResult.Success("风扇控制插件已就绪（${formatBytes(size)}），可单独安装")
        } else {
            OpResult.Failed("风扇控制插件下载失败，请检查网络或下载地址后重试")
        }
    }

    fun testConn() = launchOp(OpId.TESTCONN, requiresConnection = true) {
        val fields = _uiState.value.fields
        val result = WorkerTestConn(
            ssh = ssh,
            onLog = { appendLog(it) },
        ).execute(
            ip = fields.ip.trim(),
            user = fields.user.trim(),
            port = fields.port.toIntOrNull() ?: 22,
            password = fields.password,
        )
        if (result.ok) {
            val host = result.hostName.ifBlank { "路由器" }
            OpResult.Success("SSH 连接成功 · $host")
        } else {
            OpResult.Failed(result.errorMessage ?: "连接失败，请检查 IP、端口和密码")
        }
    }

    /** 环境页顶部的静默连接检测：不打开日志弹窗，只更新设备名/失败原因。 */
    fun checkConnection() {
        val fields = _uiState.value.fields
        if (!fields.isComplete()) {
            _uiState.update {
                it.copy(
                    connChecking = false,
                    connChecked = true,
                    connDeviceName = null,
                    connError = fields.firstError() ?: "请先填写连接参数",
                )
            }
            return
        }
        if (_uiState.value.connChecking || _uiState.value.busy) return
        _uiState.update {
            it.copy(
                connChecking = true,
                connChecked = false,
                connDeviceName = null,
                connError = null,
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                WorkerTestConn(ssh = ssh, onLog = {}).execute(
                    ip = fields.ip.trim(),
                    user = fields.user.trim(),
                    port = fields.port.toIntOrNull() ?: 22,
                    password = fields.password,
                )
            }.getOrNull()
            _uiState.update {
                it.copy(
                    connChecking = false,
                    connChecked = true,
                    connDeviceName = result?.takeIf { r -> r.ok }
                        ?.hostName
                        ?.ifBlank { "路由器" },
                    connError = when {
                        result == null -> "连接失败，请检查网络或 SSH 配置"
                        !result.ok -> result.errorMessage ?: "连接失败，请检查 IP、端口和密码"
                        else -> null
                    },
                )
            }
        }
    }

    fun install() = launchOp(
        op = OpId.INSTALL,
        requiresConnection = true,
        precheck = {
            val state = _uiState.value
            Settings.urlError(state.kernelUrl, "内核")
                ?: Settings.urlError(state.openclashUrl, "OpenClash")
        },
    ) {
        val state = _uiState.value
        val result = WorkerInstall.execute(
            ssh = ssh,
            downloader = downloader,
            appConfig = appConfig,
            fields = state.fields,
            urls = Triple(state.kernelUrl.trim(), state.openclashUrl.trim(), state.fanUrl.trim()),
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        when {
            result.getOrNull() == true -> OpResult.Success(
                "OpenClash 已安装，请在路由器后台「服务 → OpenClash」完成订阅与启动",
            )

            result.isFailure -> OpResult.Failed(
                result.exceptionOrNull()?.toUserMessage("安装异常，请重试") ?: "安装异常，请重试",
            )

            else -> OpResult.Failed("安装未完成，请查看日志中的红色错误行后重试")
        }
    }

    fun installFan() = launchOp(
        op = OpId.FAN,
        requiresConnection = true,
        precheck = { Settings.urlError(_uiState.value.fanUrl, "风扇控制") },
    ) {
        val state = _uiState.value
        val result = WorkerFan.execute(
            ssh = ssh,
            downloader = downloader,
            appConfig = appConfig,
            fields = state.fields,
            fanUrl = state.fanUrl.trim(),
            onLog = { appendLog(it) },
            onProgress = { appendProgressLog(it) },
        )
        when {
            result.getOrNull() == true -> OpResult.Success(
                "风扇控制已安装，请到 GL 管理界面「系统 → 风扇控制」查看",
            )

            result.isFailure -> OpResult.Failed(
                result.exceptionOrNull()?.toUserMessage("风扇控制安装异常，请重试") ?: "风扇控制安装异常，请重试",
            )

            else -> OpResult.Failed("风扇控制安装未完成，请查看日志后重试")
        }
    }

    fun unlockHidden() = launchOp(OpId.UNLOCK_HIDDEN, requiresConnection = true) {
        val state = _uiState.value
        val result = WorkerUnlockHidden.execute(
            ssh = ssh,
            fields = state.fields,
            onLog = { appendLog(it) },
        )
        when {
            result.getOrNull() == true -> OpResult.Success(
                "隐藏菜单已解锁，请重新登录 GL 管理后台查看",
            )

            result.isFailure -> OpResult.Failed(
                result.exceptionOrNull()?.toUserMessage("解锁异常，请重试") ?: "解锁异常，请重试",
            )

            else -> OpResult.Failed("解锁未完成，请查看日志后重试")
        }
    }

    fun fixUsbTethering() = launchOp(OpId.USB_TETHERING, requiresConnection = true) {
        val state = _uiState.value
        val result = WorkerUsbTethering.execute(
            ssh = ssh,
            fields = state.fields,
            appContext = appContext,
            filesDir = appConfig.rootDir,
            onLog = { appendLog(it) },
        )
        when {
            result.getOrNull() == true -> OpResult.Success(
                "修复模块已安装，重启路由器后 USB 共享网络即可恢复",
            )

            result.isFailure -> OpResult.Failed(
                result.exceptionOrNull()?.toUserMessage("USB 修复异常，请重试") ?: "USB 修复异常，请重试",
            )

            else -> OpResult.Failed("USB 修复未完成，请查看日志后重试")
        }
    }

    /** 失败后从日志弹窗一键重试。 */
    fun retry(op: OpId) {
        when (op) {
            OpId.DL_KERNEL -> downloadKernel()
            OpId.DL_OPENCLASH -> downloadOpenclash()
            OpId.DL_FAN -> downloadFan()
            OpId.TESTCONN -> testConn()
            OpId.INSTALL -> install()
            OpId.FAN -> installFan()
            OpId.UNLOCK_HIDDEN -> unlockHidden()
            OpId.USB_TETHERING -> fixUsbTethering()
        }
    }

    /** 关闭日志弹窗（任务进行中拒绝，避免误以为任务已结束）。 */
    fun closeOperation() {
        if (_uiState.value.busy) {
            notify("任务正在运行，请先强制结束再关闭日志", NoticeLevel.WARNING)
            return
        }
        _uiState.update { it.copy(operationOpen = false) }
    }

    /** 从结果卡片重新打开日志。 */
    fun reopenOperation() {
        val state = _uiState.value
        if (state.operationOpen) return
        if (state.logEntries.isEmpty() && state.outcome == null) {
            notify("还没有可查看的日志", NoticeLevel.INFO)
            return
        }
        val title = state.outcome?.title ?: state.operationTitle.ifBlank { "运行日志" }
        _uiState.update { it.copy(operationOpen = true, operationTitle = title) }
    }

    fun dismissOutcome() = _uiState.update { it.copy(outcome = null) }

    fun openConfirmAbort() {
        val state = _uiState.value
        if (!state.busy) {
            notify("当前没有正在运行的任务", NoticeLevel.INFO)
            return
        }
        if (state.aborting) return
        _uiState.update { it.copy(confirmAbortOpen = true) }
    }

    fun closeConfirmAbort() = _uiState.update { it.copy(confirmAbortOpen = false) }

    /**
     * 强制结束。
     *
     * 只负责取消：状态由任务协程的 finally 统一收口，避免出现
     * “按钮已变回空闲、后台其实还在写文件”的竞态。
     */
    fun abort() {
        val state = _uiState.value
        if (!state.busy) {
            notify("当前没有正在运行的任务", NoticeLevel.INFO)
            return
        }
        if (state.aborting) return
        _uiState.update { it.copy(aborting = true, confirmAbortOpen = false) }
        currentJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ssh.cancel() }
        }
    }

    // ----- 外观设置（即时生效并持久化） -----

    fun setThemeMode(mode: ThemeMode) {
        if (_uiState.value.themeMode == mode) return
        _uiState.update { it.copy(themeMode = mode) }
        persistTheme()
    }

    fun setMonetEnabled(enabled: Boolean) {
        if (_uiState.value.monetEnabled == enabled) return
        _uiState.update { it.copy(monetEnabled = enabled) }
        persistTheme()
    }

    private fun persistTheme() {
        viewModelScope.launch {
            runCatching {
                val current = settingsStore.snapshot()
                settingsStore.save(
                    current.copy(
                        themeMode = _uiState.value.themeMode,
                        monetEnabled = _uiState.value.monetEnabled,
                    ),
                )
            }.onFailure {
                notify("外观设置保存失败：${it.toUserMessage("请稍后重试")}", NoticeLevel.WARNING)
            }
        }
    }

    fun setBlurEnabled(enabled: Boolean) {
        if (_uiState.value.blurEnabled == enabled) return
        _uiState.update { it.copy(blurEnabled = enabled) }
        persistAppearance()
    }

    fun setFloatingBottomBar(enabled: Boolean) {
        if (_uiState.value.floatingBottomBar == enabled) return
        _uiState.update { it.copy(floatingBottomBar = enabled) }
        persistAppearance()
    }

    fun setGlassEnabled(enabled: Boolean) {
        if (_uiState.value.glassEnabled == enabled) return
        _uiState.update { it.copy(glassEnabled = enabled) }
        persistAppearance()
    }

    fun setPageSwitchGesture(enabled: Boolean) {
        if (_uiState.value.pageSwitchGesture == enabled) return
        _uiState.update { it.copy(pageSwitchGesture = enabled) }
        persistAppearance()
    }

    fun setUiScale(scale: Float) {
        val normalized = scale.coerceIn(0.8f, 1.1f)
        if (_uiState.value.uiScale == normalized) return
        _uiState.update { it.copy(uiScale = normalized) }
        persistAppearance()
    }

    private fun persistAppearance() {
        viewModelScope.launch {
            runCatching {
                val current = settingsStore.snapshot()
                settingsStore.save(
                    current.copy(
                        blurEnabled = _uiState.value.blurEnabled,
                        floatingBottomBar = _uiState.value.floatingBottomBar,
                        glassEnabled = _uiState.value.glassEnabled,
                        pageSwitchGesture = _uiState.value.pageSwitchGesture,
                        uiScale = _uiState.value.uiScale,
                    ),
                )
            }.onFailure {
                notify("外观设置保存失败：${it.toUserMessage("请稍后重试")}", NoticeLevel.WARNING)
            }
        }
    }

    // ----- 设置保存 -----

    /**
     * 校验并保存设置。
     *
     * @return true 表示校验通过、保存已启动；UI 等 [UiEvent.SettingsSaved] 再切回安装页。
     */
    fun saveSettings(
        kernelUrl: String,
        openclashUrl: String,
        fanUrl: String,
        fields: ConnFields,
    ): Boolean {
        if (_uiState.value.busy) {
            notify("任务正在运行，请结束当前任务后再保存设置", NoticeLevel.WARNING)
            return false
        }
        val settings = Settings(
            kernelUrl = kernelUrl.trim(),
            openclashUrl = openclashUrl.trim(),
            fanUrl = fanUrl.trim(),
            fields = fields.normalized(),
            themeMode = _uiState.value.themeMode,
            monetEnabled = _uiState.value.monetEnabled,
            blurEnabled = _uiState.value.blurEnabled,
            floatingBottomBar = _uiState.value.floatingBottomBar,
            glassEnabled = _uiState.value.glassEnabled,
            pageSwitchGesture = _uiState.value.pageSwitchGesture,
            uiScale = _uiState.value.uiScale,
        )
        settings.firstUrlError()?.let {
            notify("设置未保存：$it", NoticeLevel.WARNING)
            return false
        }
        settings.fields.firstError()?.let {
            notify("设置未保存：$it", NoticeLevel.WARNING)
            return false
        }

        viewModelScope.launch {
            try {
                settingsStore.save(settings)
                _uiState.update {
                    it.copy(
                        fields = settings.fields,
                        kernelUrl = settings.kernelUrl,
                        openclashUrl = settings.openclashUrl,
                        fanUrl = settings.fanUrl,
                    )
                }
                _events.send(UiEvent.Notice("设置已保存", NoticeLevel.SUCCESS))
                _events.send(UiEvent.SettingsSaved)
                refreshEnvSync()
            } catch (e: Throwable) {
                _events.send(
                    UiEvent.Notice(
                        "设置保存失败：${e.toUserMessage("请稍后重试")}",
                        NoticeLevel.ERROR,
                    ),
                )
            }
        }
        return true
    }

    /** 保存连接参数（二级页面里的弹窗保存，不触发页面切换）。 */
    fun saveConnectionFields(fields: ConnFields): Boolean {
        if (_uiState.value.busy) {
            notify("任务正在运行，请结束当前任务后再保存", NoticeLevel.WARNING)
            return false
        }
        val normalized = fields.normalized()
        normalized.firstError()?.let {
            notify("连接参数未保存：$it", NoticeLevel.WARNING)
            return false
        }
        _uiState.update {
            it.copy(
                fields = normalized,
                connChecked = false,
                connChecking = false,
                connDeviceName = null,
                connError = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val current = settingsStore.snapshot()
                settingsStore.save(current.copy(fields = normalized))
            }.onSuccess {
                _events.send(UiEvent.Notice("连接参数已保存", NoticeLevel.SUCCESS))
                refreshEnvSync()
            }.onFailure {
                notify("连接参数保存失败：${it.toUserMessage("请稍后重试")}", NoticeLevel.WARNING)
            }
        }
        return true
    }

    /** 保存下载地址（二级页面里的弹窗保存，不触发页面切换）。 */
    fun saveDownloadUrls(
        kernelUrl: String,
        openclashUrl: String,
        fanUrl: String,
    ): Boolean {
        if (_uiState.value.busy) {
            notify("任务正在运行，请结束当前任务后再保存", NoticeLevel.WARNING)
            return false
        }
        val k = kernelUrl.trim()
        val o = openclashUrl.trim()
        val f = fanUrl.trim()
        Settings(kernelUrl = k, openclashUrl = o, fanUrl = f, fields = _uiState.value.fields)
            .firstUrlError()?.let {
                notify("下载地址未保存：$it", NoticeLevel.WARNING)
                return false
            }
        _uiState.update { it.copy(kernelUrl = k, openclashUrl = o, fanUrl = f) }
        viewModelScope.launch {
            runCatching {
                val current = settingsStore.snapshot()
                settingsStore.save(current.copy(kernelUrl = k, openclashUrl = o, fanUrl = f))
            }.onSuccess {
                _events.send(UiEvent.Notice("下载地址已保存", NoticeLevel.SUCCESS))
            }.onFailure {
                notify("下载地址保存失败：${it.toUserMessage("请稍后重试")}", NoticeLevel.WARNING)
            }
        }
        return true
    }

    // ----- 本地导入 -----

    private data class ImportResult(
        val name: String,
        val kind: String,
        val bytes: Long,
        val replaced: Boolean,
    )

    /**
     * 从手机本地导入 ipk / tar.gz 到 app filesDir（顶栏导入按钮回调）。
     *
     * 规则：
     *  · `.tar.gz` → Clash 内核，统一重命名为 KERNEL_FILE
     *  · `gl-fanctrl*.ipk` → 风扇控制插件，统一重命名为 FAN_IPK_FILE
     *  · `luci-app-openclash_*_all.ipk` 或其它 `.ipk` → 保留原名
     *  · 其它扩展名直接拒绝，并提示支持范围
     */
    fun importLocalFile(uri: Uri) {
        if (_uiState.value.importing) {
            notify("正在导入文件，请稍候", NoticeLevel.INFO)
            return
        }
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { importInternal(uri) }
                val suffix = if (imported.replaced) "，已覆盖同名文件" else ""
                notify(
                    "已导入${imported.kind}：${imported.name}（${formatBytes(imported.bytes)}）$suffix",
                    NoticeLevel.SUCCESS,
                )
            } catch (e: Throwable) {
                notify("导入失败：${e.toUserMessage("请确认文件可读且格式正确")}", NoticeLevel.ERROR)
            } finally {
                _uiState.update { it.copy(importing = false) }
                refreshEnvSync()
            }
        }
    }

    private fun importInternal(uri: Uri): ImportResult {
        val resolver = appContext.contentResolver
        val displayName = queryDisplayName(resolver, uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: throw IllegalArgumentException("无法获取文件名")
        val name = sanitizeFilename(displayName)
        val lower = name.lowercase()
        require(lower.endsWith(".ipk") || lower.endsWith(".tar.gz")) {
            "仅支持 .ipk 或 .tar.gz 文件"
        }

        val destName = canonicalDestName(name)
        val destination = File(appConfig.rootDir, destName)
        val replaced = destination.isFile
        val partial = File(appConfig.rootDir, "$destName.importing")

        try {
            var total = 0L
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "内容提供者打开失败" }
                FileOutputStream(partial).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        out.write(buffer, 0, n)
                        total += n
                    }
                    out.flush()
                }
            }
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                if (!partial.delete()) partial.deleteOnExit()
            }
            return ImportResult(
                name = destination.name,
                kind = recognizeKind(destination.name),
                bytes = total,
                replaced = replaced,
            )
        } finally {
            if (partial.exists()) partial.delete()
        }
    }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: Uri,
    ): String? = runCatching {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun sanitizeFilename(raw: String): String {
        val cleaned = raw
            .replace('/', '_')
            .replace('\\', '_')
            .replace('\u0000', '_')
            .trim()
        if (cleaned.isEmpty()) return "imported.ipk"
        return if (cleaned.length <= 120) cleaned else cleaned.takeLast(120)
    }

    private fun canonicalDestName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".tar.gz") -> Constants.KERNEL_FILE
            lower == Constants.FAN_IPK_FILE.lowercase() -> Constants.FAN_IPK_FILE
            lower.startsWith("gl-fanctrl") && lower.endsWith(".ipk") -> Constants.FAN_IPK_FILE
            else -> name
        }
    }

    private fun recognizeKind(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".tar.gz") -> "Clash 内核"
            lower.matches(Regex("""luci-app-openclash_.+_all\.ipk""")) -> "OpenClash 插件"
            lower == Constants.FAN_IPK_FILE.lowercase() -> "风扇控制插件"
            lower.endsWith(".ipk") -> "插件包（未识别）"
            else -> "文件"
        }
    }

    // ----- 日志 -----

    private data class PreparedLog(val raw: String, val entry: LogEntry)

    /** 普通日志：完整缓冲 + 界面过滤后追加。 */
    suspend fun appendLog(line: String, kind: LogKind = LogKind.NORMAL) {
        val prepared = prepareLog(line, kind) ?: return
        fullLog.append(prepared.raw)
        appendEntry(prepared.entry)
    }

    /** 取消路径专用：不依赖可取消的挂起函数，确保「已取消」一定能写进日志。 */
    private fun appendLogNow(line: String, kind: LogKind = LogKind.NORMAL) {
        val prepared = prepareLog(line, kind) ?: return
        fullLog.appendSync(prepared.raw)
        appendEntry(prepared.entry)
    }

    private fun prepareLog(line: String, kind: LogKind): PreparedLog? {
        val clean = line.trimEnd()
        if (clean.isBlank()) return null
        val time = LocalTime.now().format(TIME_FORMAT)
        val display = LogFilter.processLine(clean) ?: return null
        val entry = LogEntry(
            id = nextId(),
            time = time,
            text = display,
            kind = classifyKind(display, kind),
        )
        return PreparedLog(raw = "[$time] $display", entry = entry)
    }

    private fun appendEntry(entry: LogEntry) {
        _uiState.update { state ->
            state.copy(logEntries = (state.logEntries + entry).takeLast(MAX_UI_LOG_LINES))
        }
    }

    /** 进度日志：同 key 覆盖最后一条，原地刷新。 */
    suspend fun appendProgressLog(line: String, key: String = "PROGRESS") {
        val clean = line.trimEnd()
        if (clean.isBlank()) return
        val time = LocalTime.now().format(TIME_FORMAT)
        fullLog.append("[$time] $clean")
        val percent = PERCENT_REGEX.find(clean)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
        _uiState.update { state ->
            val list = state.logEntries.toMutableList()
            val index = list.indexOfLast { it.progressKey == key }
            if (index >= 0) {
                // 同一进度行复用时移到末尾：一次任务里可能有多次下载，
                // 保证「当前进度」始终紧贴最新日志，也便于状态卡读取百分比。
                val updated = list.removeAt(index).copy(
                    time = time,
                    text = clean,
                    progressPercent = percent,
                )
                list += updated
            } else {
                list += LogEntry(
                    id = nextId(),
                    time = time,
                    text = clean,
                    kind = LogKind.PROGRESS,
                    progressKey = key,
                    progressPercent = percent,
                )
            }
            state.copy(logEntries = list.takeLast(MAX_UI_LOG_LINES))
        }
    }

    private fun classifyKind(text: String, requested: LogKind): LogKind {
        if (requested != LogKind.NORMAL) return requested
        val trimmed = text.trimStart()
        val first = trimmed.firstOrNull()
        return when {
            first == '✓' -> LogKind.SUCCESS
            first == '✗' || first == '×' -> LogKind.ERROR
            first == '⚠' || first == '!' -> LogKind.WARN
            trimmed.startsWith("步骤") || STEP_REGEX.containsMatchIn(trimmed) -> LogKind.STEP
            (trimmed.startsWith("---") && trimmed.endsWith("---")) ||
                (trimmed.startsWith("===") && trimmed.endsWith("===")) -> LogKind.SECTION

            else -> LogKind.NORMAL
        }
    }

    // ----- 日志分享 -----

    /** 生成带版本 / 时间 / 设备信息的完整日志文本。 */
    private fun buildLogReport(): String {
        val body = fullLog.snapshotSync()
        val report = buildString {
            appendLine("OpenClash 安装器 · 运行日志")
            appendLine("版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("时间：${LocalDateTime.now().format(REPORT_TIME_FORMAT)}")
            appendLine("设备：Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
            appendLine("─".repeat(36))
            if (body.isBlank()) appendLine("（本次运行还没有日志）") else append(body)
        }
        return if (report.length > MAX_SHARE_CHARS) {
            report.take(MAX_SHARE_CHARS) + "\n…（日志过长，已截断）"
        } else {
            report
        }
    }

    /** 通过系统分享面板发送完整日志。 */
    fun shareLog() {
        if (fullLog.snapshotSync().isBlank()) {
            notify("暂无可分享的日志，请先运行一次任务", NoticeLevel.INFO)
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "OpenClash 安装器运行日志")
            putExtra(Intent.EXTRA_TEXT, buildLogReport())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            appContext.startActivity(
                Intent.createChooser(intent, "分享运行日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            notify("分享失败：${it.toUserMessage("请稍后重试")}", NoticeLevel.ERROR)
        }
    }

    // ----- 提示 -----

    private fun notify(message: String, level: NoticeLevel = NoticeLevel.INFO) {
        _events.trySend(UiEvent.Notice(message, level))
    }

    override fun onCleared() {
        currentJob?.cancel()
        ssh.disconnect()
        super.onCleared()
    }

    private companion object {
        const val MAX_UI_LOG_LINES = 2000
        const val MAX_SHARE_CHARS = 200_000
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val REPORT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val PERCENT_REGEX = Regex("""(\d{1,3})\s*%""")
        val STEP_REGEX = Regex("""^\[\d+/\d+]""")
    }
}
