package com.chenfa.openclashinstaller.domain

import com.chenfa.openclashinstaller.core.ext.formatDuration
import com.chenfa.openclashinstaller.core.ext.toUserMessage
import com.chenfa.openclashinstaller.data.model.ConnTestResult
import com.chenfa.openclashinstaller.data.repo.SshClient

/**
 * 连接测试用例。
 *
 * 除了验证密码能否登录，还会读取主机名与系统版本，
 * 让用户确认自己连到的是哪一台路由器。
 */
class WorkerTestConn(
    private val ssh: SshClient,
    private val onLog: suspend (String) -> Unit,
) {
    suspend fun execute(ip: String, user: String, port: Int, password: String): ConnTestResult {
        onLog("开始连接测试")
        onLog("· 目标：$user@$ip:$port")
        val started = System.currentTimeMillis()

        val conn = ssh.connect(user, ip, port, password)
        if (conn.isFailure) {
            val error = conn.exceptionOrNull()
            val message = error?.toUserMessage("连接失败，请检查 IP、端口与 root 密码")
                ?: "连接失败，请检查 IP、端口与 root 密码"
            onLog("✗ 连接失败：$message")
            ssh.disconnect()
            return ConnTestResult(false, "", "", message)
        }

        return try {
            val cmd = "echo CONN_OK; echo HOST=\$(uname -n); echo UNAME=\$(uname -a)"
            val res = ssh.execCommand(cmd, timeoutMs = 20_000L)
            if (res.isFailure) {
                val error = res.exceptionOrNull()
                val message = error?.toUserMessage("命令执行失败，请重试") ?: "命令执行失败，请重试"
                onLog("✗ 连接已建立，但读取设备信息失败：$message")
                return ConnTestResult(false, "", "", message)
            }

            val (code, out) = res.getOrThrow()
            if (code != 0) {
                onLog("✗ 连接测试失败（exit=$code），请确认账号具有 root 权限")
                return ConnTestResult(false, "", "", "exit=$code")
            }
            if (!out.contains("CONN_OK")) {
                onLog("✗ 连接测试失败：未收到路由器响应")
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

            onLog("✓ SSH 连接成功 · 用时 ${formatDuration(System.currentTimeMillis() - started)}")
            if (host.isNotEmpty()) onLog("· 主机名：$host")
            if (uname.isNotEmpty()) onLog("· 系统：$uname")
            ConnTestResult(true, host, uname, null)
        } finally {
            ssh.disconnect()
        }
    }
}
