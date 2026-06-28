import json
import asyncio

from opencode_service import run_agent_lines


async def _send(websocket, msg_type: str, content: str = "", success: bool = None):
    payload = {"type": msg_type, "content": content}
    if success is not None:
        payload["success"] = success
    await websocket.send_text(json.dumps(payload))
    await asyncio.sleep(0.01)


async def run_agent(websocket, prompt: str):
    try:
        await _send(websocket, "agent_start")
        async for line in run_agent_lines(prompt):
            if isinstance(line, tuple):
                tag, value = line[0], line[1]
                if tag == "__RESULT__":
                    if value == 0:
                        await _send(websocket, "agent_result", "Agent completed successfully", success=True)
                    else:
                        await _send(websocket, "agent_result", f"Agent exited with code {value}", success=False)
                elif tag == "__ERROR__":
                    await _send(websocket, "agent_result", str(value), success=False)
            else:
                await _send(websocket, "agent_status", line)
    except Exception as e:
        await _send(websocket, "agent_result", str(e), success=False)
