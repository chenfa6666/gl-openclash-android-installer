package com.chenfa.openclashinstaller.data.model

/**
 * 环境检查项。等价 Windows 版左侧 3-4 行环境检查 yes/no。
 *
 * @param name  显示名（如 "JSch 库就绪"、"clash-linux-arm64.tar.gz"）
 * @param ok    true=绿 yes / false=红 no
 */
data class EnvItem(
    val name: String,
    val ok: Boolean,
)
