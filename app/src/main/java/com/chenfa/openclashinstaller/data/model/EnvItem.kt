package com.chenfa.openclashinstaller.data.model

/**
 * 环境检查项。
 *
 * @param name   显示名（如 "Clash 内核"）
 * @param ok     true=就绪 / false=缺失
 * @param detail 补充说明：文件名、大小或缺失提示
 */
data class EnvItem(
    val name: String,
    val ok: Boolean,
    val detail: String? = null,
)
