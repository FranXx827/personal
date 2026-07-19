"""LangGraph Agent 状态 (TypedDict)。"""
from __future__ import annotations

from typing import Annotated, Any, Literal, TypedDict

from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages

IntentType = Literal["consult", "cart", "after_sale", "chat", "unknown"]


class AgentState(TypedDict, total=False):
    # LangGraph 内置 messages reducer (合并而非覆盖)
    messages: Annotated[list[BaseMessage], add_messages]

    session_id: str
    user_id: int | None
    intent: IntentType
    tool_results: list[dict[str, Any]]
    user_profile: dict[str, Any]  # 从 Redis 加载
    reflection: str | None        # ReAct 自愈阶段的反思
    retry_count: int              # 工具调用重试计数
    cancelled: bool               # 用户是否主动中断
