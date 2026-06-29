import asyncio
import re
import os
from pathlib import Path

OPENCODE_BIN = os.environ.get("OPENCODE_BIN", "opencode")
_ANSI_RE = re.compile(r'\x1b\[[0-9;]*m')

PROJECT_ROOT = str(Path(__file__).resolve().parent.parent)


def _clean(text: str) -> str:
    return _ANSI_RE.sub("", text)


async def run_agent_lines(prompt: str, model: str = "", session_id: str = ""):
    workdir = os.environ.get("OPENCODE_WORKDIR", PROJECT_ROOT)
    cmd = [OPENCODE_BIN, "run"]
    if session_id:
        cmd.extend(["--session", session_id])
    if model:
        model_arg = model if "/" in model else f"deepseek/{model}"
        cmd.extend(["--model", model_arg])
    cmd.append(prompt)

    proc = await asyncio.create_subprocess_exec(
        *cmd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.STDOUT,
        cwd=workdir
    )

    buffer = ""
    try:
        while True:
            chunk = await proc.stdout.read(256)
            if not chunk:
                break
            text = chunk.decode("utf-8", errors="replace")
            buffer += text
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                clean = _clean(line).strip()
                if clean:
                    yield clean
        if buffer.strip():
            clean = _clean(buffer).strip()
            if clean:
                yield clean
        await proc.wait()
        yield ("__RESULT__", proc.returncode)
    except Exception as e:
        yield ("__ERROR__", str(e))
