"""Supervisor 节点：用 LLM 做意图识别并路由。"""
from __future__ import annotations

from langchain_core.messages import HumanMessage, SystemMessage

from app.agents.state import AgentState, IntentType
from app.llm.client import get_llm
from app.llm.prompts.supervisor import SUPERVISOR_PROMPT

_INTENT_MAP: dict[str, IntentType] = {
    "consult": "consult",
    "商品咨询": "consult",
    "cart": "cart",
    "加入购物车": "cart",
    "after_sale": "after_sale",
    "售后": "after_sale",
}


async def supervisor_node(state: AgentState) -> AgentState:
    messages = state.get("messages", [])
    if not messages:
        return {**state, "intent": "unknown"}

    last_user_msg = next((m.content for m in reversed(messages) if m.type == "human"), "")
    if not last_user_msg:
        return {**state, "intent": "unknown"}

    llm = get_llm(temperature=0)
    intent = await llm.ainvoke(
        [SystemMessage(content=SUPERVISOR_PROMPT), HumanMessage(content=last_user_msg)]
    )
    return {**state, "intent": _INTENT_MAP.get(intent.content.strip().lower(), "unknown")}


def route_by_intent(state: AgentState) -> str:
    intent = state.get("intent", "unknown")
    return intent if intent in {"consult", "cart", "after_sale"} else "unknown"
