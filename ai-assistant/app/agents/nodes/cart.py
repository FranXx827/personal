"""加购 Agent：调用 cart_add / coupon_match 工具并综合作答。"""
from __future__ import annotations

from langchain_core.messages import AIMessage

from app.agents.nodes.executor import run_tools
from app.agents.state import AgentState
from app.tools.registry import tool_registry


async def cart_node(state: AgentState) -> AgentState:
    tools = tool_registry.list_for_intent("cart")
    if not tools:
        return {**state, "messages": state["messages"] + [AIMessage(content="加购服务暂不可用。")]}
    return await run_tools(state, tools)
