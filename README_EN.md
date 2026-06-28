# CodePocketAndroid

[English] | [简体中文](README.md)

![License](https://img.shields.io/github/license/Shixiaoshi0417/CodePocketAndroid)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)
![Python](https://img.shields.io/badge/Python-3.13-blue?logo=python)
![Gradle](https://img.shields.io/badge/Gradle-8.10-02303A?logo=gradle)

Android client for controlling OpenCode running on Linux hosts from your phone.

## Features

- OpenCode Agent — send prompts and receive streaming responses
- Session management — browse, create, switch, and delete sessions
- DeepSeek model — read-only model display, synced from OpenCode
- Real-time streaming via WebSocket
- Markdown rendering — code blocks, tables, bold, italic, lists
- Room database — local message persistence

## Screenshots

| Main Screen | Chat Screen |
|:---:|:---:|
| ![Main Screen](docs/screenshot1.png) | ![Chat Screen](docs/screenshot2.png) |

## Requirements

- Android 8.0+ (API 26)
- Java 21+
- Android SDK
- Linux environment (backend) with Python 3.11+
- [OpenCode](https://opencode.ai) CLI installed

Tested on Debian and Ubuntu. Any Linux distribution with Python 3.11+ and OpenCode CLI should work.

## Architecture

```
┌──────────────────┐
│  Android Client  │  Kotlin + Jetpack Compose
└────────┬─────────┘
         │ WebSocket
┌────────▼─────────┐
│ FastAPI Backend   │  Python, port 8765
└────────┬─────────┘
         │ subprocess
┌────────▼─────────┐
│  OpenCode CLI     │  opencode run
└────────┬─────────┘
         │ API
┌────────▼─────────┐
│    DeepSeek       │  AI model
└──────────────────┘
```

## Quick Start

```bash
git clone https://github.com/Shixiaoshi0417/CodePocketAndroid.git
cd CodePocketAndroid
```

### Backend

The backend listens on `http://0.0.0.0:8765` by default, with the WebSocket endpoint at `/ws`.

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

### Android

Prebuilt APKs are available on [GitHub Releases](https://github.com/Shixiaoshi0417/CodePocketAndroid/releases).

To build from source:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Start OpenCode on your Linux machine
2. Start the backend: `python server.py`
3. Install the APK on your Android device
4. Configure the server address in the app: `ws://<server-ip>:8765/ws`
5. Select an existing session or create a new one
6. Type a prompt and tap Send — OpenCode Agent responds in real-time

## Project Structure

```
app/          Android app (Kotlin, Compose, Room, OkHttp)
backend/      FastAPI WebSocket server (Python)
docs/         Documentation
```

## License

Apache License 2.0
