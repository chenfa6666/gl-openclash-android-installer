package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.data.model.ConnTestResult
import com.chenfa.openclashinstaller.data.repo.SshClient

/**
 * 连接测试用例。
 * 等价 Windows 版 WorkerTestConn：
 *   ssh ... "echo CONN_OK; echo HOST=$(uname -n); echo UNAME=$(uname -a)"
 *   解析 CONN_OK / HOST= / UNAME=
 */
class WorkerTestConn(
    private val ssh: SshClient,
    private val onLog: suspend (String) -> Unit,
) {
    suspend fun execute(ip: String, user: String, port: Int, password: String): ConnTestResult {
        onLog("========== 连接测试 ==========")
        onLog("→ 正在连接 $user@$ip:$port ...")

        val conn = ssh.connect(user, ip, port, password)
        if (conn.isFailure) {
            val msg = conn.exceptionOrNull()?.message ?: "连接失败"
            onLog("✗ 连接失败: $msg")
            ssh.disconnect()
            return ConnTestResult(false, "", "", msg)
        }

        val cmd = "echo CONN_OK; echo HOST=\$(uname -n); echo UNAME=\$(uname -a)"
        val res = ssh.execCommand(cmd)
        ssh.disconnect()

        if (res.isFailure) {
            val msg = res.exceptionOrNull()?.message ?: "命令执行失败"
            onLog("✗ 命令执行失败: $msg")
            return ConnTestResult(false, "", "", msg)
        }

        val (code, out) = res.getOrThrow()
        if (code != 0) {
            onLog("✗ 连接测试失败 (exit=$code)")
            return ConnTestResult(false, "", "", "exit=$code")
        }

        if (!out.contains("CONN_OK")) {
            onLog("✗ 连接测试失败：未收到 CONN_OK")
            return ConnTestResult(false, "", "", "未收到 CONN_OK")
        }

        var host = ""
        var uname = ""
        out.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("HOST=") -> host = trimmed.removePrefix("HOST=").trim()
                trimmed.startsWith("UNAME=") -> uname = trimmed.removePrefix("UNAME=").trim()
            }
        }
        onLog("✓ 连接成功")
        if (host.isNotEmpty()) onLog("  · 主机名: $host")
        if (uname.isNotEmpty()) onLog("  · 系统: $uname")
        return ConnTestResult(true, host, uname, null)
    }
}
