# OpenClash for GL 设备 - Android 端一键部署工具

本项目提供基于 Android 设备通过 SSH/SCP 协议为 GL.iNet 系列路由器一键部署 OpenClash 代理的完整解决方案，同时集成中文管理界面功能解锁、第三方风扇控制插件安装等增强功能，全程无需电脑，移动端即可完成全部操作。

## ✨ 功能特性

- **一键安装 OpenClash**：通过 SSH + SCP 远程推送并安装 OpenClash 插件及其依赖。
- **中文界面解锁**：解锁官方固件隐藏的中文管理界面，开启完整功能入口
- **风扇控制插件**：集成第三方 `gl-fanctrl` 风扇调速插件，支持自定义温控策略
- **KernelSU 风格界面**：环境 / 安装 / 设置三页，平板左侧导航栏或液态玻璃悬浮底栏，主题三档 + Monet、真模糊、页面缩放与滑动切换
- **可读的任务日志**：分级着色、步骤标题、时间戳、下载进度、失败重试与一键分享完整日志

## 🧱 技术栈

- Kotlin + Jetpack Compose（Android）
- Miuix KMP（HyperOS / MIUI 风格组件、图标、Preference）
- JSch（mwiede fork，SSH / SCP）
- OkHttp（安装包下载，支持取消与临时文件原子替换）
- Kotlin Coroutines（任务编排、取消传播）
- AndroidX DataStore（本机保存连接信息与下载地址）

## 🎨 界面说明

界面视觉与 Miuix 页面结构参考 KernelSU 的 Manager（GPL-3.0）：大标题 TopAppBar、分组 Card、平板左侧 NavigationRail、液态玻璃悬浮底栏、主题设置 TabRow + Monet 开关、真模糊顶/底栏、页面滑动切换与界面缩放。液态玻璃悬浮底栏、液态镜片/高光/拖拽动画直接移植 KernelSU 的 Miuix 组件。模糊依赖 Miuix Blur，需要 Android 13+ 且设备支持 RenderEffect。KernelSU 与本项目均以 GPL-3.0 发布，特此致谢。

### 页面结构

- **环境**：连接检测（显示设备名或失败原因）、环境检查、安装包状态与下载
- **安装**：开始安装、测试连接、风扇控制、解锁隐藏菜单、USB 共享网络修复
- **设置**：主题设置、连接参数、下载地址、分享日志、关于

### 默认下载

- OpenClash：`https://raw.githubusercontent.com/vernesong/OpenClash/package/master/luci-app-openclash_0.48.002_all.ipk`
- Clash 内核：`https://raw.githubusercontent.com/vernesong/OpenClash/core/master/meta/clash-linux-arm64.tar.gz`

## 📋 前置要求

### 设备端

- GL.iNet 系列路由器（已测试 GL-MT3600BE ）
- 设备已开启 SSH 权限，已知 root 密码
- 固件版本为官方
- 设备与 Android 手机处于同一局域网

### 手机端

- Android 9.0 及以上系统
