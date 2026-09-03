package com.chenfa.openclashinstaller

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * Application 入口。
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Log.i(TAG, "OpenClash Installer 启动")
    }

    companion object {
        private const val TAG = "OpenClash"
        lateinit var appContext: Context
            private set
    }
}
