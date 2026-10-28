package com.chenfa.openclashinstaller.data.model

import com.chenfa.openclashinstaller.core.Constants

/** 设置：3 个下载 URL + 连接字段。 */
data class Settings(
    val kernelUrl: String = Constants.DEF_URL_KERNEL,
    val openclashUrl: String = Constants.DEF_URL_OPENCLASH,
    val fanUrl: String = Constants.DEF_URL_FAN,
    val fields: ConnFields = ConnFields(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val monetEnabled: Boolean = false,
    val blurEnabled: Boolean = false,
    val floatingBottomBar: Boolean = false,
    val glassEnabled: Boolean = true,
    val pageSwitchGesture: Boolean = true,
    val uiScale: Float = 1f,
) {
    /** 第一个 URL 错误；null 表示全部合法。 */
    fun firstUrlError(): String? =
        urlError(kernelUrl, "内核") ?: urlError(openclashUrl, "OpenClash") ?: urlError(fanUrl, "风扇控制")

    /** 校验 URL 是否以 http:// 或 https:// 开头且可解析。 */
    fun isUrlsValid(): Boolean = firstUrlError() == null

    companion object {
        fun urlError(value: String, label: String = "下载"): String? {
            val text = value.trim()
            if (text.isEmpty()) return "请输入${label}下载地址"
            if (!text.startsWith("http://") && !text.startsWith("https://")) {
                return "${label}地址需以 http:// 或 https:// 开头"
            }
            val uri = runCatching { java.net.URI(text) }.getOrNull()
                ?: return "${label}地址格式不正确"
            if (uri.host.isNullOrBlank()) return "${label}地址缺少域名或 IP"
            return null
        }
    }
}
