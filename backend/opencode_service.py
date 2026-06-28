import asyncio
import re
import os
from pathlib import Path

OPENCODE_BIN = os.environ.get("OPENCODE_BIN", "opencode")
_ANSI_RE = re.compile(r'\x1b\[[0-9;]*m')

PROJECT_ROOT = str(Path(__file__).resolve().parent.parent)


def _clean(text: str) -> str:
    return _ANSI_RE.sub("", text).strip()


async def run_agent_lines(prompt: str, model: str = "", session_id: str = ""):
    workdir = os.environ.get("OPENCODE_WORKDIR", PROJECT_ROOT)
    cmd = [OPENCODE_BIN, "run"]
    if session_id:
        cmd.extend(["--session", session_id])
    if model:
        if "/" not in model:
            model_arg = f"deepseek/{model}"
        else:
            model_arg = model
        cmd.extend(["--model", model_arg])
    cmd.append(prompt)
    proc = await asyncio.create_subprocess_exec(
        *cmd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.STDOUT,
        cwd=workdir
    )
    try:
        while True:
            line = await proc.stdout.readline()
            if not line:
                break
            text = line.decode("utf-8", errors="replace")
            clean = _clean(text)
            if clean:
                yield clean
        await proc.wait()
        yield ("__RESULT__", proc.returncode)
    except Exception as e:
        yield ("__ERROR__", str(e))
