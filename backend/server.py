import json

import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from agent_service import run_agent

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
                if msg_type == "agent":
                    prompt = data.get("prompt", "")
                    try:
                        await run_agent(websocket, prompt)
                    except Exception as e:
                        await websocket.send_text(json.dumps({
                            "type": "agent_result",
                            "success": False,
                            "content": str(e)
                        }))
                else:
                    await websocket.send_text(json.dumps({
                        "type": "error",
                        "message": f"Unknown message type: {msg_type}"
                    }))
            except json.JSONDecodeError:
                await websocket.send_text(json.dumps({
                    "type": "error",
                    "message": "Invalid JSON"
                }))
    except WebSocketDisconnect:
        pass
    except Exception:
        try:
            await websocket.close()
        except Exception:
            pass


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8765)
