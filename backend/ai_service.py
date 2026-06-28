import asyncio


async def chat(message: str) -> str:
    return f"AI收到：{message}"


async def stream_chat(message: str):
    yield "AI"
    await asyncio.sleep(0.3)
    yield "收到"
    await asyncio.sleep(0.3)
    yield f"：{message}"
