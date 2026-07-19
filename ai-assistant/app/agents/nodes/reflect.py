"""Reflect 节点 (ReAct 自愈)：检测工具失败，重试或降级。"""
from __future__ import annotations

from langchain_core.messages import AIMessage

from app.agents.nodes.executor import retry_failed
from app.agents.state import AgentState

MAX_RETRY = 2


async def reflect_node(state: AgentState) -> AgentState:
    retry = state.get("retry_count", 0)
    tool_results = state.get("tool_results", [])
    has_failure = any(r.get("status") == "error" for r in tool_results[-3:])

    if not has_failure:
        return {**state, "retry_count": 0, "reflection": None}

    if retry >= MAX_RETRY:
        # 兜底：正常路径由 route_after_reflect 先转 degrade，不会进入此分支
        return {**state, "reflection": "max_retry_reached"}

    new_state = await retry_failed(state)
    return {**new_state, "reflection": "retry_attempted"}


async def degrade_node(state: AgentState) -> AgentState:
    """重试耗尽后的优雅降级，给出明确提示。"""
    return {
        **state,
        "messages": state["messages"]
        + [AIMessage(content="抱歉，工具调用暂时不可用，请稍后再试或换个问题。")],
        "reflection": "max_retry_reached",
    }


def route_after_reflect(state: AgentState) -> str:
    """失败且未达上限则重试，达上限则降级，否则结束。"""
    retry = state.get("retry_count", 0)
    tool_results = state.get("tool_results", [])
    has_failure = any(r.get("status") == "error" for r in tool_results[-3:])
    if has_failure and retry < MAX_RETRY:
        return "reflect"
    if has_failure:
        return "degrade"
    return "end"
