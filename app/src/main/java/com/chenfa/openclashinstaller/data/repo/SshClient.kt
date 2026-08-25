package com.chenfa.openclashinstaller.data.repo

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
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
 */
class SshClient(
    private val connectTimeoutMs: Int = 8000,
) {
    private var session: Session? = null

    /** 等价 ssh -p port user@ip，密码认证。 */
    suspend fun connect(user: String, ip: String, port: Int, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val jsch = JSch()
                val s = jsch.getSession(user, ip, port)
                s.setPassword(password)
                // JSch 无 accept-new，no 等价 StrictHostKeyChecking=accept-new
                s.setConfig("StrictHostKeyChecking", "no")
                s.setConfig("PreferredAuthentications", "password,keyboard-interactive")
                s.connect(connectTimeoutMs)
                session = s
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    /**
     * 执行单条命令。
     * 等价 ssh ... "cmd"。
     *
     * @return (exitCode, stdout) exitCode=-1 表示 channel 异常退出未拿到 exit status
     */
    suspend fun execCommand(cmd: String): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext Result.failure(IllegalStateException("SSH 未连接"))
        try {
            val ch = s.openChannel("exec") as ChannelExec
            ch.setCommand(cmd)
            // stderr 不混入 stdout（保持校验逻辑纯净）；用空丢弃流，
            // 不用 java.io.OutputStream.nullOutputStream()（Java 11+ API，Android minSdk 28 没有）
            ch.setErrStream(NullOutputStream())
            ch.connect(connectTimeoutMs)
            try {
                val out = ch.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                coroutineContext.ensureActive()
                val code = ch.exitStatus
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
