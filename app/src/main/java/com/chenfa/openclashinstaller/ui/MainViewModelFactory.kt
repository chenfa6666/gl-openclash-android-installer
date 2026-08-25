package com.chenfa.openclashinstaller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chenfa.openclashinstaller.App
import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.data.FileChecker
import com.chenfa.openclashinstaller.data.FullLogBuffer
import com.chenfa.openclashinstaller.data.SettingsStore
import com.chenfa.openclashinstaller.data.repo.Downloader
import com.chenfa.openclashinstaller.data.repo.SshClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * ViewModel 工厂：手动注入依赖。
 *
 * 阶段 C：新增 SshClient + Downloader + AppConfig 注入。
 */
object MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = App.appContext
        val config = AppConfig(app.filesDir)
        val okClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)  // 流式下载，不超时
            .build()
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            appContext = app,
            settingsStore = SettingsStore(app),
            fileChecker = FileChecker(config),
            fullLog = FullLogBuffer(),
            ssh = SshClient(),
            downloader = Downloader(okClient),
            appConfig = config,
        ) as T
    }
}
