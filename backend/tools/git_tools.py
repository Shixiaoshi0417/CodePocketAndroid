import subprocess
from pathlib import Path

WORK_DIR = Path("/home/Shixiaoshi0417/CodePocketAndroid").resolve()


def _run_git(args: list) -> dict:
    try:
        result = subprocess.run(
            ["git"] + args,
            cwd=str(WORK_DIR),
            capture_output=True,
            text=True,
            timeout=30
        )
        output = result.stdout.strip()
        if result.returncode != 0:
            error_msg = result.stderr.strip() or output or "Unknown git error"
            return {"success": False, "error": error_msg}
        return {"success": True, "data": output}
    except FileNotFoundError:
        return {"success": False, "error": "Git not found"}
    except subprocess.TimeoutExpired:
        return {"success": False, "error": "Git command timed out"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def git_status() -> dict:
    result = _run_git(["status", "--short"])
    if result["success"] and not result["data"]:
        result["data"] = "No changes"
    return result


def git_diff() -> dict:
    return _run_git(["diff"])
