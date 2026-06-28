import subprocess
from pathlib import Path

WORK_DIR = Path("/home/Shixiaoshi0417/CodePocketAndroid").resolve()


def _is_safe(path: Path) -> bool:
    try:
        path.resolve().relative_to(WORK_DIR)
        return True
    except ValueError:
        return False


def run_command(command: str) -> dict:
    try:
        result = subprocess.run(
            command,
            shell=True,
            cwd=str(WORK_DIR),
            capture_output=True,
            text=True,
            timeout=60
        )
        stdout = result.stdout.strip()
        stderr = result.stderr.strip()
        data = stdout if stdout else stderr
        return {
            "success": result.returncode == 0,
            "data": data if data else "(no output)"
        }
    except subprocess.TimeoutExpired:
        return {"success": False, "error": "Command timed out"}
    except Exception as e:
        return {"success": False, "error": str(e)}
