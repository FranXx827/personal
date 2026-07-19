"""Supervisor 节点：用 LLM 做意图识别并路由 + 加载用户画像。"""
from __future__ import annotations

import json

from langchain_core.messages import HumanMessage, SystemMessage

from app.agents.state import AgentState, IntentType
from app.llm.client import get_llm
from app.llm.prompts.supervisor import SUPERVISOR_PROMPT
from app.memory.long_term import memory as long_term_memory

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

    last_user_msg = next(
        (m.content for m in reversed(messages) if m.type == "human"), ""
    )
    if not last_user_msg:
        return {**state, "intent": "unknown"}

    # ---- 加载用户画像并构建个性化 System Prompt ----
    user_id = state.get("user_id")
    profile_context = ""
    if user_id is not None:
        try:
            # 从 state 拿已经加载过的，否则重新查
            profile = state.get("user_profile") or await long_term_memory.get(user_id)
            if profile:
                profile_context = (
                    "\n\n【用户画像】\n"
                    + json.dumps(profile, ensure_ascii=False, indent=2)
                )
        except Exception:
            pass  # 画像加载失败不阻塞主流程

    system_prompt = SUPERVISOR_PROMPT + profile_context

    llm = get_llm(temperature=0)
    intent_resp = await llm.ainvoke(
        [
            SystemMessage(content=system_prompt),
            HumanMessage(content=last_user_msg),
        ]
    )

    intent = _INTENT_MAP.get(intent_resp.content.strip().lower(), "unknown")
    return {**state, "intent": intent}


def route_by_intent(state: AgentState) -> str:
    intent = state.get("intent", "unknown")
    return intent if intent in {"consult", "cart", "after_sale"} else "unknown"
