# CodePocketAndroid

![License](https://img.shields.io/github/license/Shixiaoshi0417/CodePocketAndroid)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-76%25-purple)
![Python](https://img.shields.io/badge/Python-23%25-blue)

Android client for controlling OpenCode from your phone.

## Features

- Chat with OpenCode Agent from Android
- Session management (create, switch, delete sessions)
- DeepSeek model integration
- Real-time streaming via WebSocket
- Markdown rendering (code blocks, tables, bold, lists)
- Model display (read-only, synced from OpenCode)

## Architecture

```
Android App (Kotlin + Jetpack Compose)
    ↕ WebSocket
FastAPI Backend (Python)
    ↕ subprocess
OpenCode CLI (opencode run)
    ↕ API
DeepSeek
```

## Requirements

- Android 8.0+
- Debian/Ubuntu (for backend)
- Python 3.11+
- OpenCode installed (`curl -fsSL https://opencode.ai/install | bash`)

## Quick Start

**Backend:**
```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

**Android:**
```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/          Android app (Kotlin, Compose, Room)
backend/      FastAPI WebSocket server (Python)
docs/         Documentation
```

## License

Apache License 2.0
