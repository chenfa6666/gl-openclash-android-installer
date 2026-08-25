package com.chenfa.openclashinstaller

import android.app.Application
import timber.log.Timber

/**
 * Application 入口。
 *
 * 阶段 A：仅初始化 Timber 日志。
 * 后续阶段会在此挂载 AppScope（SupervisorJob）等单例依赖。
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("OpenClash Installer 启动")
    }
}
