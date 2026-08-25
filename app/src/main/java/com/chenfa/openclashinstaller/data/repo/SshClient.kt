package com.chenfa.openclashinstaller.data.repo

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * JSch 封装。
 *
 * 关键决策：SCP 用 ChannelExec + `cat > /tmp/file` 二进制流，**不**用 ChannelSftp.put。
 * 原因：用户的 OpenWrt 路由器没装 openssh-sftp-server.ipk（/usr/libexec/sftp-server: not found），
 * ChannelSftp.put 会失败。改用 ChannelExec 调用远端 cat（OpenWrt busybox cat 二进制安全）。
 *
 * 等价 Windows 版 ConPTY + ssh.exe + 应答 password: 的全部职责。
 *
 * 取消传播：协程 cancel 时 channel.connect/readBytes 抛异常，finally 调 disconnect。
 *
 * 关键：UserInfo 必须设置，否则 OpenWrt dropbear 默认的 keyboard-interactive 认证无法响应 challenge，
 * 会导致 "Auth fail" 连接测试失败。
 */
class SshClient(
    private val connectTimeoutMs: Int = 8000,
) {
    private var session: Session? = null

    /** 等价 ssh -p port user@ip，密码认证（含 keyboard-interactive）。 */
    suspend fun connect(user: String, ip: String, port: Int, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val jsch = JSch()
                val s = jsch.getSession(user, ip, port)
                s.setPassword(password)
                // dropbear 默认走 keyboard-interactive；SimpleUserInfo.getPassword + promptPassword=true 双路响应
                s.userInfo = SimpleUserInfo(password)
                s.setConfig("StrictHostKeyChecking", "no")
                s.setConfig("PreferredAuthentications", "password,keyboard-interactive,publickey")
                s.connect(connectTimeoutMs)
                session = s
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    /**
     * 执行单条命令，等价 ssh ... "cmd"。
     *
     * 实现：`ch.setOutputStream(baos)` 让 JSch 内部 IO 线程直接把远端 stdout 写进 ByteArrayOutputStream，
     * 我们不再自行读 InputStream（JSch+dropbear 上 isEOF/available/read 都有竞态）。
     * 只轮询 `ch.isClosed`：为 true 时所有字节已写完 + exitStatus 已可用（JSch 文档约定）。
     *
     * @param timeoutMs 最长等待远端 channel 关闭；超时后把已收到的字节 + exitStatus=-1 返回。
     * @return (exitCode, stdout) exitCode=-1 表示超时/异常未拿到 exit status
     */
    suspend fun execCommand(
        cmd: String,
        timeoutMs: Long = 15_000L,
    ): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext Result.failure(IllegalStateException("SSH 未连接"))
        try {
            val ch = s.openChannel("exec") as ChannelExec
            ch.setCommand(cmd)
            // stderr 不混入 stdout（保持校验逻辑纯净）
            ch.setErrStream(NullOutputStream())
            // stdout 交给 JSch 内部 IO 线程直接写 baos，不再自行读 inputStream
            val baos = java.io.ByteArrayOutputStream(8192)
            ch.setOutputStream(baos)
            ch.connect(connectTimeoutMs)
            try {
                val t0 = System.currentTimeMillis()
                var waits = 0
                while (!ch.isClosed) {
                    coroutineContext.ensureActive()
                    if (System.currentTimeMillis() - t0 > timeoutMs) break
                    Thread.sleep(20)
                    waits++
                }
                val code = ch.exitStatus
                val out = baos.toString(Charsets.UTF_8.name())
                Result.success(code to out)
            } finally {
                ch.disconnect()
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * SCP 上传：ChannelExec + cat 二进制流。
     * 等价 scp -O -P port local remote（绕 sftp-server 缺失）。
     *
     * @param localPath 本地文件
     * @param remotePath 远端绝对路径，如 /tmp/clash-linux-arm64.tar.gz
     */
    suspend fun scpUpload(localPath: String, remotePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val s = session ?: return@withContext Result.failure(IllegalStateException("SSH 未连接"))
            try {
                val ch = s.openChannel("exec") as ChannelExec
                // cat > '...' 落盘；远端 cat 二进制安全，能处理 ipk/tar.gz
                ch.setCommand("cat > '${remotePath.replace("'", "'\\''")}'")
                val out = ch.getOutputStream()  // 写远端 stdin
                ch.connect(connectTimeoutMs)
                try {
                    File(localPath).inputStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            out.flush()
                        }
                    }
                    out.close()  // 发 EOF，cat 落盘
                    coroutineContext.ensureActive()
                    Result.success(Unit)
                } finally {
                    ch.disconnect()
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    /** 关闭会话。 */
    fun disconnect() {
        session?.disconnect()
        session = null
    }

    /** 取消钩子：等价 Windows 版 TerminateProcess(pid)。 */
    fun cancel() {
        disconnect()
    }
}

/** 丢弃 stderr 的 OutputStream（替代 Java 11 nullOutputStream，兼容 Android minSdk 28）。 */
private class NullOutputStream : java.io.OutputStream() {
    override fun write(b: Int) {}
    override fun write(b: ByteArray, off: Int, len: Int) {}
}

/**
 * 简单 UserInfo：响应 keyboard-interactive challenge（dropbear 默认认证方式）。
 *
 * - promptPassword / promptYesNo 返回 true：自动接受
 * - promptPassphrase 返回 false：不使用 passphrase
 * - showMessage：空实现，不打扰 UI（日志由 WorkerTestConn 控制）
 *
 * 不实现 UIKeyboardInteractiveManager（JSch 默认会回退到 UserInfo.getPassword）
 */
private class SimpleUserInfo(private val password: String) : UserInfo {
    override fun getPassphrase(): String? = null
    override fun getPassword(): String = password
    override fun promptPassword(message: String?): Boolean = true
    override fun promptPassphrase(message: String?): Boolean = false
    override fun promptYesNo(message: String?): Boolean = true
    override fun showMessage(message: String?) {}
}
