import json
import asyncio
import sys

from opencode_service import run_agent_lines


async def _send(websocket, msg_type: str, content: str = "", success: bool = None):
    payload = {"type": msg_type, "content": content}
    if success is not None:
        payload["success"] = success
    await websocket.send_text(json.dumps(payload))


async def run_agent(websocket, prompt: str, model: str = "", session_id: str = ""):
    try:
        print(f"[AGENT] Starting: prompt={prompt[:60]} model={model} session={session_id}", flush=True)
        await _send(websocket, "agent_start")
        async for line in run_agent_lines(prompt, model, session_id):
            if isinstance(line, tuple):
                tag, value = line[0], line[1]
                if tag == "__RESULT__":
                    if value == 0:
                        await _send(websocket, "agent_result", "Agent completed successfully", success=True)
                    else:
                        await _send(websocket, "agent_result", f"Agent exited with code {value}", success=False)
                    print(f"[AGENT] Done: exit_code={value}", flush=True)
                elif tag == "__ERROR__":
                    await _send(websocket, "agent_result", str(value), success=False)
                    print(f"[AGENT] Error: {value}", flush=True)
            else:
                await _send(websocket, "agent_status", line)
    except Exception as e:
        await _send(websocket, "agent_result", str(e), success=False)
        print(f"[AGENT] Exception: {e}", flush=True)
