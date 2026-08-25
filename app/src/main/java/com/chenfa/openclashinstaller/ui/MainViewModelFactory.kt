package com.chenfa.openclashinstaller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chenfa.openclashinstaller.App
import com.chenfa.openclashinstaller.core.AppConfig
import com.chenfa.openclashinstaller.data.FileChecker
import com.chenfa.openclashinstaller.data.FullLogBuffer
import com.chenfa.openclashinstaller.data.SettingsStore

/**
 * ViewModel 工厂：手动注入依赖（暂未用 Hilt，阶段 B 简单方案）。
 *
 * 阶段 C+：SshClient / Downloader / InstallOrchestrator 加入后扩展此工厂。
 */
object MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = App.appContext
        val config = AppConfig(app.filesDir)
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            settingsStore = SettingsStore(app),
            fileChecker = FileChecker(config),
            fullLog = FullLogBuffer(),
        ) as T
    }
}
