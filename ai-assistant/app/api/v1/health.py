"""健康检查与就绪探针。"""
from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from app.core.config import settings

router = APIRouter()


@router.get("/health")
async def health() -> dict[str, Any]:
    return {
        "code": 0,
        "message": "ok",
        "data": {"service": settings.app_name, "env": settings.app_env, "version": "1.0.0"},
    }


@router.get("/ready")
async def ready() -> dict[str, Any]:
    return {"code": 0, "message": "ok", "data": {"status": "ready"}}
