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

    // #region debug-point A:jsch-logger 初始化 JSch DEBUG Logger（用于采集 H1/H2/H3 证据）
    companion object {
        private var jschLoggerInit: Boolean = false
        private fun ensureJschLogger() {
            if (jschLoggerInit) return
            jschLoggerInit = true
            try {
                JSch.setLogger(object : com.jcraft.jsch.Logger {
                    override fun isEnabled(level: Int): Boolean = true
                    override fun log(level: Int, message: String?) {
                        val tag = when (level) {
                            com.jcraft.jsch.Logger.DEBUG -> "DEBUG"
                            com.jcraft.jsch.Logger.INFO -> "INFO"
                            com.jcraft.jsch.Logger.WARN -> "WARN"
                            com.jcraft.jsch.Logger.ERROR -> "ERROR"
                            com.jcraft.jsch.Logger.FATAL -> "FATAL"
                            else -> "LV$level"
                        }
                        // 证据路径：logcat（Android Studio Logcat 或 adb logcat 能抓到）
                        val prio = when (level) {
                            com.jcraft.jsch.Logger.ERROR, com.jcraft.jsch.Logger.FATAL -> android.util.Log.ERROR
                            com.jcraft.jsch.Logger.WARN -> android.util.Log.WARN
                            else -> android.util.Log.DEBUG
                        }
                        android.util.Log.println(prio, "JSch", "[$tag] ${message ?: "(null)"}")
                        System.err.println("[JSch][$tag] ${message ?: "(null)"}")
                    }
                })
            } catch (t: Throwable) { System.err.println("[JSch logger init fail] ${t.message}") }
        }
    }
    // #endregion

    /** 等价 ssh -p port user@ip，密码认证（含 keyboard-interactive）。 */
    suspend fun connect(user: String, ip: String, port: Int, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                ensureJschLogger()
                // #region debug-point B:connect-args 打印连接参数（脱敏密码，用于 H3/H5）
                android.util.Log.d("SshClient", "[DEBUG ssh-connect-fail] connect(user=$user, ip=$ip, port=$port, pwdLen=${password.length})")
                // #endregion
                val jsch = JSch()
                val s = jsch.getSession(user, ip, port)
                s.setPassword(password)
                // 关键：dropbear 用 keyboard-interactive，UserInfo 响应 challenge
                s.userInfo = SimpleUserInfo(password)
                s.setConfig("StrictHostKeyChecking", "no")
                s.setConfig("PreferredAuthentications", "password,keyboard-interactive,publickey")
                // #region debug-point C:config-snapshot 快照 Config（用于 H3 StrictHostKeyChecking=no 是否生效）
                android.util.Log.d("SshClient", "[DEBUG ssh-connect-fail] StrictHostKeyChecking=${s.getConfig("StrictHostKeyChecking")} PreferredAuths=${s.getConfig("PreferredAuthentications")}")
                // #endregion
                s.connect(connectTimeoutMs)
                // #region debug-point D:connect-ok 连接成功标记（用于排除 H5）
                android.util.Log.d("SshClient", "[DEBUG ssh-connect-fail] connect OK: clientVersion=${s.clientVersion} serverVersion=${s.serverVersion}")
                // #endregion
                session = s
                Result.success(Unit)
            } catch (e: Throwable) {
                // #region debug-point E:connect-fail 失败堆栈（用于最终判定 5 个假设）
                val sb = StringBuilder()
                sb.append("[DEBUG ssh-connect-fail] connect FAIL: type=${e.javaClass.name} msg=${e.message}\n")
                var t: Throwable? = e
                var depth = 0
                while (t != null && depth < 6) {
                    sb.append("  cause[$depth]: ${t.javaClass.name}: ${t.message}\n")
                    t.cause?.stackTrace?.take(4)?.forEach { sb.append("    at $it\n") }
                    t = t.cause
                    depth++
                }
                android.util.Log.e("SshClient", sb.toString())
                // #endregion
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
                // 等 channel 完全关闭才能拿 exitStatus（JSch 文档：exitStatus 在 channel closed 后才可用）
                val t0 = System.currentTimeMillis()
                while (!ch.isClosed && System.currentTimeMillis() - t0 < 5000) {
                    Thread.sleep(50)
                }
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
