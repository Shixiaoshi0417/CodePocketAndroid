import os

DEEPSEEK_API_KEY = os.environ.get("DEEPSEEK_API_KEY")
if not DEEPSEEK_API_KEY:
    raise RuntimeError(
        "DEEPSEEK_API_KEY is not set. "
        "Please set the environment variable: export DEEPSEEK_API_KEY=sk-xxx"
    )
