package com.chenfa.openclashinstaller.data.model

import com.chenfa.openclashinstaller.core.Constants

/**
 * 路由器连接字段：IP / 用户名 / 密码 / 端口。
 *
 * 校验放在模型层，UI 与 ViewModel 共用同一套错误文案，
 * 避免「点按钮才弹一句请填写设置」这种模糊提示。
 */
data class ConnFields(
    val ip: String = Constants.DEFAULT_IP,
    val user: String = Constants.DEFAULT_USER,
    val password: String = Constants.DEFAULT_PASSWORD,
    val port: String = Constants.DEFAULT_PORT,
) {
    fun ipError(): String? {
        val value = ip.trim()
        return when {
            value.isEmpty() -> "请输入路由器 IP 地址"
            IPV4_REGEX.matches(value) || HOSTNAME_REGEX.matches(value) || value.contains(':') -> null
            else -> "IP 格式不正确，例如 192.168.8.1 或 router.local"
        }
    }

    fun userError(): String? =
        if (user.isBlank()) "请输入 SSH 用户名（通常为 root）" else null

    fun passwordError(): String? =
        if (password.isEmpty()) "请输入路由器 root 密码" else null

    fun portError(): String? {
        val value = port.trim()
        val number = value.toIntOrNull()
        return when {
            value.isEmpty() -> "请输入 SSH 端口"
            number == null || number !in 1..65535 -> "端口应为 1–65535，默认 22"
            else -> null
        }
    }

    /** 第一个错误；null 表示全部合法。 */
    fun firstError(): String? =
        ipError() ?: userError() ?: passwordError() ?: portError()

    /** 任一字段为空或格式错误时返回 false。 */
    fun isComplete(): Boolean = firstError() == null

    /** 去掉首尾空白，保存 / 连接前使用。 */
    fun normalized(): ConnFields = copy(
        ip = ip.trim(),
        user = user.trim(),
        port = port.trim(),
    )

    private companion object {
        val IPV4_REGEX = Regex(
            """^(?:(?:25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)$""",
        )
        val HOSTNAME_REGEX = Regex(
            """^(?=.{1,253}$)(?=.*[A-Za-z])(?!-)[A-Za-z0-9-]{1,63}(?<!-)(?:\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$""",
        )
    }
}
