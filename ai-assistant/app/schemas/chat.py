"""对话相关的 Pydantic 模型。"""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant", "system", "tool"]
    content: str = ""
    name: str | None = None
    metadata: dict[str, Any] | None = None


class ChatRequest(BaseModel):
    session_id: str | None = None
    message: str = Field(..., min_length=1, max_length=4000)
    history: list[ChatMessage] | None = None
    # user_id 由 JWT 自动注入，前端无需传递
    user_id: int | None = None


class ToolInvokeRequest(BaseModel):
    name: str
    arguments: dict = Field(default_factory=dict)


class ApiResponse(BaseModel):
    code: int = 0
    message: str = "ok"
    data: object | None = None


class SessionVO(BaseModel):
    """会话列表返回结构。"""
    id: str
    title: str
    created_at: str
    updated_at: str
    message_count: int = 0


class MessageVO(BaseModel):
    """单条消息返回结构。"""
    id: str
    role: str
    content: str
    metadata: dict[str, Any] | None = None
    created_at: str


ChatRequest.model_rebuild()
