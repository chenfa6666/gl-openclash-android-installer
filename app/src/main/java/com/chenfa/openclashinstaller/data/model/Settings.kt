package com.chenfa.openclashinstaller.data.model

import com.chenfa.openclashinstaller.core.Constants

/** 设置：3 个下载 URL + 输入字段，用于设置对话框编辑。 */
data class Settings(
    val kernelUrl: String = Constants.DEF_URL_KERNEL,
    val openclashUrl: String = Constants.DEF_URL_OPENCLASH,
    val fanUrl: String = Constants.DEF_URL_FAN,
    val fields: ConnFields = ConnFields(),
) {
    /** 校验 URL 是否以 http:// 或 https:// 开头。 */
    fun isUrlsValid(): Boolean =
        kernelUrl.startsWith("http") &&
            openclashUrl.startsWith("http") &&
            fanUrl.startsWith("http")
}
