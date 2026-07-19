"""闲聊 Agent: 直接 LLM 对话，不调用工具"""
from __future__ import annotations

from app.agents.state import AgentState
from app.llm.client import get_llm


async def chat_node(state: AgentState) -> AgentState:
    llm = get_llm()
    response = await llm.ainvoke(state["messages"])
    return {**state, "messages": state["messages"] + [response]}
