package com.chenfa.openclashinstaller.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chenfa.openclashinstaller.core.Constants
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.data.model.Settings
import com.chenfa.openclashinstaller.data.model.ThemeMode
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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MONET_ENABLED = booleanPreferencesKey("monet_enabled")
        val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
        val FLOATING_BOTTOM_BAR = booleanPreferencesKey("floating_bottom_bar")
        val GLASS_ENABLED = booleanPreferencesKey("glass_enabled")
        val PAGE_SWITCH_GESTURE = booleanPreferencesKey("page_switch_gesture")
        val UI_SCALE = floatPreferencesKey("ui_scale")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            kernelUrl = p[Keys.KERNEL_URL] ?: Constants.DEF_URL_KERNEL,
            openclashUrl = migrateOpenclashUrl(
                p[Keys.OPENCLASH_URL] ?: Constants.DEF_URL_OPENCLASH,
            ),
            fanUrl = p[Keys.FAN_URL] ?: Constants.DEF_URL_FAN,
            fields = ConnFields(
                ip = p[Keys.IP] ?: Constants.DEFAULT_IP,
                user = p[Keys.USER] ?: Constants.DEFAULT_USER,
                password = p[Keys.PASSWORD] ?: Constants.DEFAULT_PASSWORD,
                port = p[Keys.PORT] ?: Constants.DEFAULT_PORT,
            ),
            themeMode = runCatching {
                ThemeMode.valueOf(p[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            monetEnabled = p[Keys.MONET_ENABLED] ?: false,
            blurEnabled = p[Keys.BLUR_ENABLED] ?: false,
            floatingBottomBar = p[Keys.FLOATING_BOTTOM_BAR] ?: false,
            glassEnabled = p[Keys.GLASS_ENABLED] ?: true,
            pageSwitchGesture = p[Keys.PAGE_SWITCH_GESTURE] ?: true,
            uiScale = (p[Keys.UI_SCALE] ?: 1f).coerceIn(0.8f, 1.1f),
        )
    }

    /** 读一次快照。 */
    private fun migrateOpenclashUrl(url: String): String =
        if (url.contains("luci-app-openclash_0.47.156")) {
            Constants.DEF_URL_OPENCLASH
        } else {
            url
        }

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
            p[Keys.THEME_MODE] = settings.themeMode.name
            p[Keys.MONET_ENABLED] = settings.monetEnabled
            p[Keys.BLUR_ENABLED] = settings.blurEnabled
            p[Keys.FLOATING_BOTTOM_BAR] = settings.floatingBottomBar
            p[Keys.GLASS_ENABLED] = settings.glassEnabled
            p[Keys.PAGE_SWITCH_GESTURE] = settings.pageSwitchGesture
            p[Keys.UI_SCALE] = settings.uiScale.coerceIn(0.8f, 1.1f)
        }
    }
}
