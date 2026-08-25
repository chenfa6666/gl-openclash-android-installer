# 调试：ssh-connect-fail

- **状态：** [CLOSED]
- **现象：** Android 版 SshClient (JSch) 连不上 OpenWrt 路由器 192.168.8.1:22。
- **最终根因：** execCommand 读取 stdout 的三次错误实现（v0/v1/v2 都被 JSch mwiede fork 在 dropbear exec channel 上的时序问题坑）。
- **最终修复：** 采用 `ch.setOutputStream(baos)` + 只 `while(!ch.isClosed)` 等待，不再自行读 inputStream。连接测试秒过，bytes=~120 exit=0。
- **验证：** 用户 2026-08-25 确认「可以了」，真实设备 CONN_OK + HOST + UNAME 全部正确返回。

## 假设裁决

| # | 假设 | 裁决 | 证据 |
|---|---|---|---|
| H1 | keyboard-interactive challenge 未响应 | ❌ 未触发 | dropbear 接受 password 方法，`Auth succeeded password` 一步过 |
| H2 | 算法协商失败 | ❌ 未触发 | `kex: algorithm: ecdh-sha2-nistp256` + `aes128-ctr hmac-sha2-256` 全部协商成功 |
| H3 | StrictHostKeyChecking=no 未生效 | ❌ 未触发 | `Permanently added 192.168.8.1 ECDSA to known hosts`，连接完整建立 |
| H4 | 密码编码 | ❌ 未触发 | password auth 成功 |
| H5 | 网络不可达 | ❌ 未触发 | `Connection established` + `clientVersion + serverVersion` 都打了 |

**真问题在 execCommand stdout 采集**（5 个假设都没覆盖，因为假设范围只覆盖 connect 阶段；后续证据扩展到了命令执行阶段）：

| 版本 | 机制 | 症状 | 证据 |
|---|---|---|---|
| v0 | `inputStream.use { it.readBytes() }` | 阻塞 52s 后 socket 被 session 超时断开 | 19:28:16.044 connect OK → 19:29:08.898 Disconnect（52s 差） |
| v1 | `available()>0` 批量读 + `isEOF/isClosed` 退出 | `loops=1 bytes=0`（关闭标志比字节先到位） | 三次 exec 都是 exit=0 bytes=0 loops=1 |
| v2 | v1 + 20 轮 `input.read()` 兜底 | `input.read()` 在 isEOF 后仍阻塞 8.6s（JSch bug） | 19:50:14.647 connect OK → 19:50:23.263 用户手动断开（死锁） |
| **v3 最终** | `ch.setOutputStream(baos)` JSch 内部线程写 → `while(!ch.isClosed)` | 连接测试 < 200ms 过，bytes=~120 exit=0 | 用户确认「可以了」 |

## 清理

- 已删除 SshClient.kt 中 companion object 的 JSch debug logger（debug-point A）
- 已删除 connect 中 B/C/D/E 调试打印
- 已删除 execCommand 中 F:exec-wait / G:exec-fail 调试打印
- 终极修复（setOutputStream + 等 isClosed）保留为最终实现
