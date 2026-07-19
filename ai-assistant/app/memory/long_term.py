"""长期记忆：跨会话持久化用户偏好到 Redis Hash (key=user:profile:{user_id})。"""
from __future__ import annotations

import json
from typing import Any

import redis.asyncio as aioredis
import structlog

from app.core.config import settings

logger = structlog.get_logger(__name__)

_redis: aioredis.Redis | None = None
_PROFILE_TTL = 60 * 60 * 24 * 30  # 30 天


async def get_redis() -> aioredis.Redis:
    global _redis
    if _redis is None:
        _redis = aioredis.from_url(settings.redis_url, encoding="utf-8", decode_responses=True)
    return _redis


class LongTermMemory:
    KEY_PREFIX = "user:profile:"

    async def get(self, user_id: int) -> dict[str, Any]:
        r = await get_redis()
        raw = await r.get(self.KEY_PREFIX + str(user_id))
        return json.loads(raw) if raw else {}

    async def update(self, user_id: int, **fields: Any) -> None:
        r = await get_redis()
        key = self.KEY_PREFIX + str(user_id)
        existing = await self.get(user_id)
        existing.update(fields)
        await r.set(key, json.dumps(existing, ensure_ascii=False), ex=_PROFILE_TTL)
        logger.info("user_profile_updated", user_id=user_id, fields=list(fields.keys()))

    async def record_search(self, user_id: int, keyword: str) -> None:
        r = await get_redis()
        key = f"user:recent_searches:{user_id}"
        await r.lpush(key, keyword)
        await r.ltrim(key, 0, 19)  # 仅保留最近 20 条


memory = LongTermMemory()
