import json
import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from ai_service import chat, stream_chat

app = FastAPI()


@app.get("/")
async def root():
    return {"status": "ok"}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            text = await websocket.receive_text()
            try:
                data = json.loads(text)
                msg_type = data.get("type", "")
                if msg_type == "chat":
                    user_message = data.get("message", "")
                    await websocket.send_text(json.dumps({
                        "type": "start"
                    }))
                    async for token in stream_chat(user_message):
                        await websocket.send_text(json.dumps({
                            "type": "delta",
                            "content": token
                        }))
                    await websocket.send_text(json.dumps({
                        "type": "done"
                    }))
                else:
                    reply = await chat(text)
                    await websocket.send_text(json.dumps({
                        "type": "message",
                        "role": "assistant",
                        "content": reply
                    }))
            except json.JSONDecodeError:
                reply = await chat(text)
                await websocket.send_text(json.dumps({
                    "type": "message",
                    "role": "assistant",
                    "content": reply
                }))
    except WebSocketDisconnect:
        pass
    except Exception:
        await websocket.close()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8765)
