"""售后 Agent：调用 order_query / refund_apply 工具并综合作答。"""
from __future__ import annotations

from langchain_core.messages import AIMessage

from app.agents.nodes.executor import run_tools
from app.agents.state import AgentState
from app.tools.registry import tool_registry


async def after_sale_node(state: AgentState) -> AgentState:
    tools = tool_registry.list_for_intent("after_sale")
    if not tools:
        return {**state, "messages": state["messages"] + [AIMessage(content="售后查询服务暂不可用。")]}
    return await run_tools(state, tools)
