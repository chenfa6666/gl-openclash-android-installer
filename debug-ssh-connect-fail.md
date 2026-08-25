# 调试：ssh-connect-fail

- **状态：** [OPEN]
- **现象：** Android 版 SshClient (JSch) 连不上 OpenWrt 路由器 192.168.8.1:22。
- **期望：** 与 Windows 版 ConPTY + ssh.exe 一样，应能以 root/password 建立连接并执行 `echo CONN_OK; echo HOST=...; echo UNAME=...` 成功。
- **调试原则：** 证据驱动，先插桩，再修复。

## 可证伪假设

1. **H1 (JSch keyboard-interactive 不完整)**：SimpleUserInfo 没实现 `UIKeyboardInteractive`，dropbear 发起的交互式 challenge（含 "Password:" prompt）未被正确回包。证据：`JSch.getLogger` 中出现 `kex: start` → `userauth: trying keyboard-interactive` → 随后 `Auth fail` / `Session.connect: Auth cancel`。
2. **H2 (加密算法不兼容)**：OpenWrt 22/23 的 dropbear 禁用了旧算法，而 mwiede jsch 0.2.16 默认 enabled Kex/Cipher/MAC 列表与远端交集为空。证据：JSch 日志中 "kex: algorithm negotiation failed" 或 "no matching cipher found"。
3. **H3 (StrictHostKeyChecking=no 被忽略/Host Key 冲突)**：JSch 内部实现里 StrictHostKeyChecking 字符串匹配对大小写敏感，或 KnownHosts repository 里已有冲突条目。证据：`JSch.setConfig` 前后 session.getConfig("StrictHostKeyChecking") 不是 "no"，或抛 "HostKey has changed"。
4. **H4 (密码字符编码)**：JSch 默认可能用 Latin1 编码密码字节，若路由器认证期望 UTF-8 会导致密码不一致。证据：ssh.connect 使用 String password → `.getPassword()` 返回 String → dropbear 返回 `Auth fail`；Windows 版 ssh.exe 走终端交互不触发此问题。
5. **H5 (IP/端口不可达 / 网络层被拦)**：Android APP 没拿到 INTERNET 权限；或 Android 设备本身与路由器不是同一网段；或路由器防火墙禁了手机的 Wi-Fi 客户端 IP。证据：抛出 `ConnectException: Connection refused` / `SocketTimeoutException` 而非 Auth fail。

## 证据矩阵

| 假设 | 关键日志 | 插入位置 |
|---|---|---|
| H1 | kex → userauth keyboard-interactive 的 challenge prompt 与 response | JSch.setLogger + SshClient.connect 前后 |
| H2 | kex 协商失败、cipher/mac 不匹配 | JSch.setLogger (DEBUG) |
| H3 | StrictHostKeyChecking 值、HostKey 冲突异常 | session.getConfig 快照 + catch 异常 |
| H4 | 密码字节码点对比（可选） | 需额外证据，暂放二级 |
| H5 | ConnectException / SocketTimeoutException 堆栈 | connect Result.failure 捕获 |

## 步骤记录

- [ ] 步骤 1-3：列出 5 个假设（本文件）
- [ ] 步骤 4：插桩（JSch Logger + SshClient.connect 快照）
- [ ] 步骤 5：用户重跑连接测试，收集证据
- [ ] 步骤 6：按证据判定 5 个假设
- [ ] 步骤 7：最小修复
- [ ] 步骤 8-9：用户验证 post-fix
- [ ] 步骤 10：清理插桩
- [ ] 步骤 11：关闭本 session
