package com.chenfa.openclashinstaller.data.model

/** 输入字段：IP/用户名/密码/端口。 */
data class ConnFields(
    val ip: String = "192.168.8.1",
    val user: String = "root",
    val password: String = "",
    val port: String = "22",
) {
    /** 任一空时返回 false。等价 Windows 版校验。 */
    fun isComplete(): Boolean =
        ip.isNotBlank() && user.isNotBlank() && password.isNotBlank() && port.isNotBlank()
}
