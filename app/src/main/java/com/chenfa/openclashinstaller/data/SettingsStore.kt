package com.chenfa.openclashinstaller.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore 持久化：3 个下载 URL + 4 个输入字段。
 * 等价 Windows 版 INI 文件（<exeDir>\安装Openclash.ini [Download] 节）。
 */
private val Context.dataStore by preferencesDataStore(name = "openclash_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val KERNEL_URL = stringPreferencesKey("kernel_url")
        val OPENCLASH_URL = stringPreferencesKey("openclash_url")
        val FAN_URL = stringPreferencesKey("fan_url")
        val IP = stringPreferencesKey("ip")
        val USER = stringPreferencesKey("user")
        val PASSWORD = stringPreferencesKey("password")
        val PORT = stringPreferencesKey("port")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            kernelUrl = p[Keys.KERNEL_URL] ?: Constants.DEF_URL_KERNEL,
            openclashUrl = p[Keys.OPENCLASH_URL] ?: Constants.DEF_URL_OPENCLASH,
            fanUrl = p[Keys.FAN_URL] ?: Constants.DEF_URL_FAN,
            fields = ConnFields(
                ip = p[Keys.IP] ?: Constants.DEFAULT_IP,
                user = p[Keys.USER] ?: Constants.DEFAULT_USER,
                password = p[Keys.PASSWORD] ?: Constants.DEFAULT_PASSWORD,
                port = p[Keys.PORT] ?: Constants.DEFAULT_PORT,
            ),
        )
    }

    /** 读一次快照。 */
    suspend fun snapshot(): Settings = settingsFlow.first()

    /** 保存全部设置。等价 Windows 版 SaveSettings。 */
    suspend fun save(settings: Settings) {
        context.dataStore.edit { p ->
            p[Keys.KERNEL_URL] = settings.kernelUrl
            p[Keys.OPENCLASH_URL] = settings.openclashUrl
            p[Keys.FAN_URL] = settings.fanUrl
            p[Keys.IP] = settings.fields.ip
            p[Keys.USER] = settings.fields.user
            p[Keys.PASSWORD] = settings.fields.password
            p[Keys.PORT] = settings.fields.port
        }
    }
}
