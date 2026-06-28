# CodePocketAndroid

![License](https://img.shields.io/github/license/Shixiaoshi0417/CodePocketAndroid)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)
![Python](https://img.shields.io/badge/Python-3.13-blue?logo=python)
![Gradle](https://img.shields.io/badge/Gradle-8.10-02303A?logo=gradle)

Android client for controlling OpenCode from your phone.

## Features

- OpenCode Agent — send prompts, receive streaming responses
- Session management — browse, create, switch, and delete sessions
- DeepSeek model — read-only model display synced from OpenCode
- Real-time streaming via WebSocket
- Markdown rendering — code blocks, tables, bold, italic, lists
- Room database — local message persistence

## Requirements

- Android 8.0+ (API 26)
- Java 21+
- Android SDK
- Debian/Ubuntu (for backend)
- Python 3.11+
- [OpenCode](https://opencode.ai) installed

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

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

Default: `http://0.0.0.0:8765`

### Android

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or download from [GitHub Releases](https://github.com/Shixiaoshi0417/CodePocketAndroid/releases).

## Usage

1. Start OpenCode on your Debian machine
2. Start the backend: `python server.py`
3. Install the APK on your Android device
4. 在应用中设置服务器地址：`ws://<你的设备ip>:8765/ws`
5. Tap a session or create a new one
6. Type your prompt and tap Send — OpenCode Agent responds in real-time

## Project Structure

```
app/          Android app (Kotlin, Compose, Room, OkHttp)
backend/      FastAPI WebSocket server (Python)
docs/         Documentation
```

## License

Apache License 2.0
