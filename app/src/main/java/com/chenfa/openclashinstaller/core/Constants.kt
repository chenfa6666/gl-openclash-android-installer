package com.chenfa.openclashinstaller.core

/**
 * 全局常量：默认下载 URL + 本地文件名。
 * 等价 Windows 版 DEF_URL_* / FILE_*。
 */
object Constants {
    // 默认下载 URL
    const val DEF_URL_KERNEL =
        "https://raw.githubusercontent.com/vernesong/OpenClash/core/master/meta/clash-linux-arm64.tar.gz"
    const val DEF_URL_OPENCLASH =
        "https://raw.githubusercontent.com/vernesong/OpenClash/package/master/luci-app-openclash_0.47.156_all.ipk"
    const val DEF_URL_FAN =
        "https://github.com/XIAOZHAOXSXH/gl-fanctrl/releases/download/v0.1.3/gl-fanctrl_0.1.3_all.ipk"

    // 本地文件名（存放在 app filesDir）
    const val KERNEL_FILE = "clash-linux-arm64.tar.gz"
    const val OPENCLASH_IPK_DEFAULT = "luci-app-openclash_0.47.156_all.ipk"
    const val OPENCLASH_IPK_GLOB = "luci-app-openclash_*_all.ipk"
    const val FAN_IPK_FILE = "gl-fanctrl_0.1.3_all.ipk"

    // 默认输入字段
    const val DEFAULT_IP = "192.168.8.1"
    const val DEFAULT_USER = "root"
    const val DEFAULT_PASSWORD = ""
    const val DEFAULT_PORT = "22"

    // opkg 依赖列表（步骤 1/4 SSH 装依赖用）；步骤 3/4 建议也展示这串
    val OPKG_DEPS: List<String> = listOf(
        "ruby", "ruby-yaml",
        "luci-base", "luci-compat", "luci-lib-ipkg", "luci-lib-jsonc"
    )
    // 软校验"依赖装成功"时，拿最后一个包名看 opkg list-installed 里是否出现
    const val OPKG_DEPS_LAST = "luci-lib-jsonc"
}
