"""商品咨询 Agent：调用 product_search / product_detail 工具并综合作答。"""
from __future__ import annotations

from langchain_core.messages import AIMessage

from app.agents.nodes.executor import run_tools
from app.agents.state import AgentState
from app.tools.registry import tool_registry


async def consult_node(state: AgentState) -> AgentState:
    tools = tool_registry.list_for_intent("consult")
    if not tools:
        return {**state, "messages": state["messages"] + [AIMessage(content="暂无相关商品数据，请稍后再试。")]}
    return await run_tools(state, tools)
