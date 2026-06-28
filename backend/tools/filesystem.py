import os
from pathlib import Path

WORK_DIR = Path("/home/Shixiaoshi0417/CodePocketAndroid").resolve()


def _is_safe(path: Path) -> bool:
    try:
        path.resolve().relative_to(WORK_DIR)
        return True
    except ValueError:
        return False


def read_file(filepath: str) -> dict:
    target = (WORK_DIR / filepath).resolve()
    if not _is_safe(target):
        return {"success": False, "error": f"Path outside working directory: {filepath}"}
    if not target.exists():
        return {"success": False, "error": f"File not found: {filepath}"}
    if not target.is_file():
        return {"success": False, "error": f"Not a file: {filepath}"}
    try:
        content = target.read_text(encoding="utf-8")
        rel = target.relative_to(WORK_DIR)
        return {"success": True, "data": {"path": str(rel), "content": content}}
    except Exception as e:
        return {"success": False, "error": str(e)}


def write_file(filepath: str, content: str) -> dict:
    target = (WORK_DIR / filepath).resolve()
    if not _is_safe(target):
        return {"success": False, "error": f"Path outside working directory: {filepath}"}
    try:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        rel = target.relative_to(WORK_DIR)
        return {"success": True, "data": {"path": str(rel), "written": len(content)}}
    except Exception as e:
        return {"success": False, "error": str(e)}


def list_dir(directory: str = ".") -> dict:
    target = (WORK_DIR / directory).resolve()
    if not _is_safe(target):
        return {"success": False, "error": f"Path outside working directory: {directory}"}
    if not target.exists():
        return {"success": False, "error": f"Directory not found: {directory}"}
    if not target.is_dir():
        return {"success": False, "error": f"Not a directory: {directory}"}
    try:
        entries = []
        for entry in sorted(target.iterdir()):
            entry_type = "dir" if entry.is_dir() else "file"
            entries.append({"name": entry.name, "type": entry_type})
        rel = target.relative_to(WORK_DIR)
        return {"success": True, "data": {"path": str(rel), "entries": entries}}
    except Exception as e:
        return {"success": False, "error": str(e)}
