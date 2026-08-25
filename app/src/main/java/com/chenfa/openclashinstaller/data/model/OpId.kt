package com.chenfa.openclashinstaller.data.model

/** 操作类型，对应 Windows 版 enum OpId。 */
enum class OpId {
    DL_KERNEL,    // 下载内核
    DL_OPENCLASH, // 下载 openclash
    INSTALL,      // 4 步安装 OpenClash
    TESTCONN,     // 连接测试
    FAN;          // 风扇控制
    val value: Int get() = ordinal + 1
}
