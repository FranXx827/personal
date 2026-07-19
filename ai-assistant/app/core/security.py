"""服务间鉴权：AI 服务 → 电商后端，通过 X-Service-Token 头传递共享密钥。"""
from __future__ import annotations

from fastapi import Depends, Header, HTTPException, status

from app.core.config import settings


async def verify_service_token(
    x_service_token: str | None = Header(default=None, alias="X-Service-Token"),
) -> str:
    if not x_service_token or x_service_token != settings.backend_service_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid service token")
    return x_service_token


ServiceAuth = Depends(verify_service_token)
