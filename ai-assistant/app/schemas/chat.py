"""对话相关的 Pydantic 模型。"""
from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    session_id: str | None = None
    message: str = Field(..., min_length=1, max_length=4000)
    history: list[ChatMessage] | None = None


class ChatMessage(BaseModel):
    role: Literal["user", "assistant", "system", "tool"]
    content: str
    name: str | None = None


class ToolInvokeRequest(BaseModel):
    name: str
    arguments: dict = Field(default_factory=dict)


class ApiResponse(BaseModel):
    code: int = 0
    message: str = "ok"
    data: object | None = None


ChatRequest.model_rebuild()
