import sqlite3
import json
import os
import time
import uuid
from pathlib import Path
from typing import Optional

DB_PATH = Path(os.environ.get("OPENCODE_DB", "~/.local/share/opencode/opencode.db")).expanduser().resolve()


def _parse_model(model_raw: str) -> str:
    if not model_raw:
        return "deepseek-v4-pro"
    try:
        data = json.loads(model_raw)
        if isinstance(data, dict):
            return data.get("id", "deepseek-v4-pro")
        return model_raw
    except (json.JSONDecodeError, TypeError):
        return model_raw


def _get_db() -> sqlite3.Connection:
    db = sqlite3.connect(str(DB_PATH))
    db.row_factory = sqlite3.Row
    return db


def get_sessions() -> list[dict]:
    db = _get_db()
    rows = db.execute(
        "SELECT id, title, slug, directory, agent, model, time_created, time_updated "
        "FROM session WHERE time_archived IS NULL ORDER BY time_updated DESC"
    ).fetchall()
    db.close()
    return [
        {
            "id": r["id"],
            "title": r["title"] or r["slug"] or "Untitled",
            "slug": r["slug"] or "",
            "directory": r["directory"] or "",
            "agent": r["agent"] or "",
            "model": _parse_model(r["model"]),
            "time_created": r["time_created"],
            "time_updated": r["time_updated"],
        }
        for r in rows
    ]


def get_session(session_id: str) -> Optional[dict]:
    db = _get_db()
    row = db.execute(
        "SELECT id, title, slug, directory, agent, model, time_created, time_updated "
        "FROM session WHERE id = ?", (session_id,)
    ).fetchone()
    db.close()
    if not row:
        return None
    return {
        "id": row["id"],
        "title": row["title"] or row["slug"] or "Untitled",
        "slug": row["slug"] or "",
        "directory": row["directory"] or "",
        "agent": row["agent"] or "",
        "model": _parse_model(row["model"]),
        "time_created": row["time_created"],
        "time_updated": row["time_updated"],
    }


def delete_session(session_id: str) -> bool:
    db = _get_db()
    try:
        db.execute("DELETE FROM session WHERE id = ?", (session_id,))
        db.execute("DELETE FROM message WHERE session_id = ?", (session_id,))
        db.execute("DELETE FROM part WHERE session_id = ?", (session_id,))
        db.commit()
        return True
    except Exception:
        return False
    finally:
        db.close()


def rename_session(session_id: str, title: str) -> bool:
    db = _get_db()
    try:
        db.execute("UPDATE session SET title = ?, time_updated = ? WHERE id = ?",
                   (title, int(time.time() * 1000), session_id))
        db.commit()
        return True
    except Exception:
        return False
    finally:
        db.close()


def create_session(title: str = "", directory: str = "", model: str = "") -> dict:
    db = _get_db()
    sid = f"ses_{uuid.uuid4().hex[:28]}"
    now = int(time.time() * 1000)
    try:
        db.execute(
            "INSERT INTO session (id, slug, title, directory, model, time_created, time_updated) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (sid, title.lower().replace(" ", "-")[:20] or "new-chat", title or "New Chat", directory or "", model or "", now, now)
        )
        db.commit()
        return {"id": sid, "title": title or "New Chat", "directory": directory or "", "model": model or "", "time_updated": now}
    except Exception:
        return {"id": sid, "title": title or "New Chat", "directory": "", "model": "", "time_updated": now}
    finally:
        db.close()


def set_session_model(session_id: str, model: str) -> bool:
    db = _get_db()
    try:
        db.execute("UPDATE session SET model = ?, time_updated = ? WHERE id = ?",
                   (model, int(time.time() * 1000), session_id))
        db.commit()
        return True
    except Exception:
        return False
    finally:
        db.close()


def get_session_messages(session_id: str) -> list[dict]:
    db = _get_db()
    db.row_factory = sqlite3.Row
    messages = db.execute(
        "SELECT id, time_created, data FROM message WHERE session_id = ? ORDER BY time_created",
        (session_id,)
    ).fetchall()
    result = []
    for msg in messages:
        msg_data = json.loads(msg["data"]) if msg["data"] else {}
        role = msg_data.get("role", "unknown")
        parts_rows = db.execute(
            "SELECT id, time_created, data FROM part WHERE message_id = ? ORDER BY time_created",
            (msg["id"],)
        ).fetchall()
        parts = []
        for p in parts_rows:
            p_data = json.loads(p["data"]) if p["data"] else {}
            parts.append({
                "type": p_data.get("type", "unknown"),
                "text": p_data.get("text", ""),
                "tool": p_data.get("tool", ""),
            })
        result.append({
            "id": msg["id"],
            "role": role,
            "time_created": msg["time_created"],
            "parts": parts,
        })
    db.close()
    return result
