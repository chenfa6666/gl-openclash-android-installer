package com.chenfa.openclashinstaller.core.ext

import java.io.EOFException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 把底层异常翻译成「原因 + 下一步怎么做」的中文提示。
 *
 * 原则：
 *  - 先给用户能执行的建议，再在括号里保留原始信息，方便排查；
 *  - 原始信息压成单行并截断，避免把整段 HTML / 堆栈塞进提示条。
 */
fun Throwable.toUserMessage(fallback: String = "操作失败，请稍后重试"): String {
    val raw = (message ?: javaClass.simpleName)
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)

    val lower = raw.lowercase()
    val hint = when {
        this is UnknownHostException || lower.contains("unknownhost") ->
            "无法解析地址，请检查网络或下载链接"

        this is SocketTimeoutException || lower.contains("timeout") || lower.contains("timed out") ->
            "连接超时，请确认手机与路由器在同一网络"

        this is ConnectException || lower.contains("connection refused") ->
            "连接被拒绝，请确认路由器已开启 SSH 且端口正确"

        this is NoRouteToHostException ||
            lower.contains("no route to host") ||
            lower.contains("network is unreachable") ->
            "网络不可达，请检查路由器 IP 与 Wi-Fi 连接"

        lower.contains("auth fail") || lower.contains("auth cancel") ->
            "认证失败，请检查 root 密码"

        lower.contains("permission denied") ->
            "权限不足，请确认使用 root 账号"

        lower.contains("no space left") ->
            "路由器存储空间不足，请清理后重试"

        lower.contains("connection reset") || lower.contains("broken pipe") ->
            "连接被重置，请重试"

        lower.contains("socket closed") || lower.contains("socket is closed") ->
            "连接已断开，请重试"

        this is EOFException || lower.contains("eof") ->
            "连接被远端关闭，请重试"

        lower.contains("ssl") || lower.contains("certificate") || lower.contains("certpath") ->
            "HTTPS 证书校验失败，请检查系统时间或网络代理"

        lower.contains("http 404") || lower.contains(" 404 ") ->
            "下载链接不存在（404），请检查地址是否失效"

        lower.contains("http 403") || lower.contains(" 403 ") ->
            "下载被服务器拒绝（403），请稍后重试"

        lower.contains("http 5") ->
            "下载服务器暂时不可用，请稍后重试"

        lower.contains("outofmemory") ->
            "内存不足，请关闭其他应用后重试"

        else -> null
    }

    return when {
        hint == null && raw.isNotBlank() -> "$fallback（$raw）"
        hint == null -> fallback
        raw.isBlank() || raw.equals(hint, ignoreCase = true) -> hint
        else -> "$hint（$raw）"
    }
}
