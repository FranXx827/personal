"""服务间 HTTP 客户端 — 调用 Java Backend Chat API。"""
from __future__ import annotations

import json
from typing import Any

import httpx
import structlog

from app.core.config import settings

logger = structlog.get_logger(__name__)


class BackendChatClient:
    """封装对 Java Spring Backend 的 Chat API 请求，使用 X-Service-Token 鉴权。"""

    def __init__(self) -> None:
        self._base_url = settings.backend_base_url.rstrip("/")
        self._token = settings.backend_service_token
        self._timeout = settings.backend_timeout

    # ---- headers ----

    def _headers(self) -> dict[str, str]:
        return {
            "X-Service-Token": self._token,
            "Content-Type": "application/json",
        }

    # ---- 会话 ----

    async def create_session(
        self, session_id: str, user_id: int, title: str
    ) -> None:
        """POST /api/chat/sessions"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.post(
                f"{self._base_url}/chat/sessions",
                headers=self._headers(),
                json={"id": session_id, "userId": user_id, "title": title},
            )
            self._raise_if_err(resp, "create_session")

    async def list_sessions(
        self, user_id: int, limit: int = 50, offset: int = 0
    ) -> list[dict[str, Any]]:
        """GET /api/chat/sessions"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.get(
                f"{self._base_url}/chat/sessions",
                headers=self._headers(),
                params={"userId": user_id, "limit": limit, "offset": offset},
            )
            self._raise_if_err(resp, "list_sessions")
            data = self._unwrap(resp)
            return data if isinstance(data, list) else []

    async def get_session(
        self, session_id: str, user_id: int
    ) -> dict[str, Any] | None:
        """GET /api/chat/sessions/{sessionId}"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.get(
                f"{self._base_url}/chat/sessions/{session_id}",
                headers=self._headers(),
                params={"userId": user_id},
            )
            if resp.status_code == 404:
                return None
            self._raise_if_err(resp, "get_session")
            return self._unwrap(resp)

    async def update_title(
        self, session_id: str, user_id: int, title: str
    ) -> None:
        """PUT /api/chat/sessions/{sessionId}/title"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.put(
                f"{self._base_url}/chat/sessions/{session_id}/title",
                headers=self._headers(),
                params={"userId": user_id},
                json={"title": title},
            )
            self._raise_if_err(resp, "update_title")

    async def delete_session(self, session_id: str, user_id: int) -> bool:
        """DELETE /api/chat/sessions/{sessionId}"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.delete(
                f"{self._base_url}/chat/sessions/{session_id}",
                headers=self._headers(),
                params={"userId": user_id},
            )
            if resp.status_code == 404:
                return False
            self._raise_if_err(resp, "delete_session")
            return True

    # ---- 消息 ----

    async def create_message(
        self,
        session_id: str,
        message_id: str,
        role: str,
        content: str,
        metadata_json: str | None = None,
    ) -> None:
        """POST /api/chat/sessions/{sessionId}/messages"""
        body: dict[str, Any] = {
            "id": message_id,
            "role": role,
            "content": content,
        }
        if metadata_json:
            body["metadataJson"] = metadata_json
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.post(
                f"{self._base_url}/chat/sessions/{session_id}/messages",
                headers=self._headers(),
                json=body,
            )
            self._raise_if_err(resp, "create_message")

    async def batch_create_messages(
        self, session_id: str, messages: list[dict[str, Any]]
    ) -> None:
        """POST /api/chat/sessions/{sessionId}/messages/batch"""
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.post(
                f"{self._base_url}/chat/sessions/{session_id}/messages/batch",
                headers=self._headers(),
                json=[{**m, "sessionId": session_id} for m in messages],
            )
            self._raise_if_err(resp, "batch_create_messages")

    # ---- helpers ----

    def _raise_if_err(self, resp: httpx.Response, ctx: str) -> None:
        if resp.is_success:
            return
        try:
            body = resp.json()
            msg = body.get("message", resp.text)
        except Exception:
            msg = resp.text[:200]
        logger.error(
            "backend_api_error",
            context=ctx,
            status=resp.status_code,
            message=msg,
        )
        raise RuntimeError(f"Backend API error [{ctx}]: {resp.status_code} {msg}")

    def _unwrap(self, resp: httpx.Response) -> Any:
        body = resp.json()
        # 后端返回 { "code": 0, "data": ..., "message": "ok" }
        if isinstance(body, dict) and body.get("code") == 0:
            return body.get("data")
        return body


# 全局单例
backend_client = BackendChatClient()
