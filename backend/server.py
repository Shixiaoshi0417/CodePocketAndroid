import json

import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse

from agent_service import run_agent
from session_service import get_sessions, get_session, delete_session, rename_session, get_session_messages, create_session, set_session_model, clear_test_data

app = FastAPI()


@app.get("/")
async def root():
    return {"status": "ok"}


@app.get("/sessions")
async def list_sessions():
    return get_sessions()


@app.post("/sessions")
async def new_session(data: dict):
    title = data.get("title", "")
    directory = data.get("directory", "")
    model = data.get("model", "deepseek-v4-pro")
    return create_session(title, directory, model)


@app.get("/sessions/{session_id}")
async def get_one_session(session_id: str):
    s = get_session(session_id)
    if s is None:
        return JSONResponse({"error": "Not found"}, status_code=404)
    return s


@app.delete("/sessions/{session_id}")
async def del_session(session_id: str):
    ok = delete_session(session_id)
    if not ok:
        return JSONResponse({"error": "Delete failed"}, status_code=500)
    return {"ok": True}


@app.patch("/sessions/{session_id}")
async def update_session(session_id: str, data: dict):
    title = data.get("title", "")
    model = data.get("model", "")
    if title:
        ok = rename_session(session_id, title)
        if not ok:
            return JSONResponse({"error": "Rename failed"}, status_code=500)
    if model:
        ok = set_session_model(session_id, model)
        if not ok:
            return JSONResponse({"error": "Model update failed"}, status_code=500)
    return {"ok": True}


@app.post("/sessions/{session_id}/model")
async def set_model(session_id: str, data: dict):
    model = data.get("model", "")
    if not model:
        return JSONResponse({"error": "model is required"}, status_code=400)
    ok = set_session_model(session_id, model)
    if not ok:
        return JSONResponse({"error": "Model update failed"}, status_code=500)
    return {"ok": True}


@app.get("/sessions/{session_id}/messages")
async def list_session_messages(session_id: str):
    return get_session_messages(session_id)


@app.post("/admin/clear-test-data")
async def admin_clear_test_data():
    return clear_test_data()


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
                    model = data.get("model", "")
                    session_id = data.get("sessionId", "")
                    try:
                        await run_agent(websocket, prompt, model, session_id)
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
