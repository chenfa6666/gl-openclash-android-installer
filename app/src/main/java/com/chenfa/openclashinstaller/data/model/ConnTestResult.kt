package com.chenfa.openclashinstaller.data.model

/**
 * 连接测试结果。
 * 等价 Windows 版 WorkerTestConn 解析：CONN_OK / HOST= / UNAME=。
 */
data class ConnTestResult(
    val ok: Boolean,
    val hostName: String,
    val uname: String,
    val errorMessage: String? = null,
)
