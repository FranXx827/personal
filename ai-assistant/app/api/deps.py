"""FastAPI 依赖注入。"""
from __future__ import annotations

from app.core.config import Settings, get_settings


async def settings_dep() -> Settings:
    return get_settings()
