# CodePocketAndroid

[English Version →](README_EN.md)

![License](https://img.shields.io/github/license/Shixiaoshi0417/CodePocketAndroid)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)
![Python](https://img.shields.io/badge/Python-3.13-blue?logo=python)
![Gradle](https://img.shields.io/badge/Gradle-8.10-02303A?logo=gradle)

通过手机远程操控 OpenCode 的 Android 客户端。

## 功能特性

- OpenCode Agent — 发送指令并接收流式响应
- 会话管理 — 浏览、创建、切换和删除会话
- DeepSeek 模型 — 只读模型显示，与 OpenCode 自动同步
- WebSocket 实时流式通信
- Markdown 渲染 — 支持代码块、表格、粗体、斜体、列表
- Room 数据库 — 本地消息持久化存储

## 环境要求

- Android 8.0+ (API 26)
- Java 21+
- Android SDK
- Linux 环境，Python 3.11+
- 已安装 [OpenCode](https://opencode.ai) CLI

已在 Debian 和 Ubuntu 上测试通过。其他 Linux 发行版只要具备 Python 3.11+ 和 OpenCode 也可正常运行。

## 系统架构

```
┌──────────────────┐
│  Android 客户端   │  Kotlin + Jetpack Compose
└────────┬─────────┘
         │ WebSocket
┌────────▼─────────┐
│ FastAPI 后端      │  Python, 端口 8765
└────────┬─────────┘
         │ 子进程
┌────────▼─────────┐
│  OpenCode CLI     │  opencode run
└────────┬─────────┘
         │ API
┌────────▼─────────┐
│    DeepSeek       │  AI 模型
└──────────────────┘
```

## 快速开始

克隆仓库并进入项目目录：

```bash
git clone https://github.com/Shixiaoshi0417/CodePocketAndroid.git
cd CodePocketAndroid
```

### 后端

后端默认监听 `http://0.0.0.0:8765`，WebSocket 端点为 `/ws`。

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

### Android

预构建的 APK 可在 [GitHub Releases](https://github.com/Shixiaoshi0417/CodePocketAndroid/releases) 下载。

从源码构建：

```bash
./gradlew assembleDebug
# APK 路径: app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

1. 在 Linux 机器上启动 OpenCode
2. 启动后端：`python server.py`
3. 在 Android 设备上安装 APK
4. 在应用中配置 WebSocket 服务器地址：`ws://<你的设备IP>:8765/ws`
5. 选择已有会话或创建新会话
6. 输入指令并点击发送 — OpenCode Agent 实时响应

## 项目结构

```
app/          Android 应用 (Kotlin, Compose, Room, OkHttp)
backend/      FastAPI WebSocket 服务端 (Python)
docs/         文档
```

## 许可证

Apache License 2.0
