package com.chenfa.openclashinstaller

import android.app.Application
import android.content.Context
import timber.log.Timber

/**
 * Application 入口。
 *
 * 阶段 A：仅初始化 Timber 日志。
 * 阶段 B：暴露 appContext 供 ViewModelFactory 取 filesDir。
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("OpenClash Installer 启动")
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
