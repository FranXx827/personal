"""LangGraph 多 Agent 编排 — 纯内存模式（历史由前端传，持久化由 Java Backend 负责）。"""
from __future__ import annotations

from functools import lru_cache

from langgraph.graph import END, StateGraph

from app.agents.nodes.after_sale import after_sale_node
from app.agents.nodes.cart import cart_node
from app.agents.nodes.consult import consult_node
from app.agents.nodes.reflect import degrade_node, reflect_node, route_after_reflect
from app.agents.state import AgentState
from app.agents.supervisor import route_by_intent, supervisor_node


def build_graph() -> StateGraph:
    g = StateGraph(AgentState)

    g.add_node("supervisor", supervisor_node)
    g.add_node("consult", consult_node)
    g.add_node("cart", cart_node)
    g.add_node("after_sale", after_sale_node)
    g.add_node("reflect", reflect_node)
    g.add_node("degrade", degrade_node)

    g.set_entry_point("supervisor")

    g.add_conditional_edges(
        "supervisor",
        route_by_intent,
        {
            "consult": "consult",
            "cart": "cart",
            "after_sale": "after_sale",
            "unknown": "consult",
        },
    )

    g.add_edge("consult", "reflect")
    g.add_edge("cart", "reflect")
    g.add_edge("after_sale", "reflect")

    g.add_conditional_edges(
        "reflect",
        route_after_reflect,
        {"reflect": "reflect", "degrade": "degrade", "end": END},
    )
    g.add_edge("degrade", END)

    return g


@lru_cache
def get_compiled_graph():
    """获取已编译的 LangGraph（纯内存，无外部 checkpointer）。

    对话历史由前端传递 + Java Backend 持久化，
    长期记忆由 app.memory.long_term 通过 Redis 单独管理（无需 RedisJSON）。
    """
    return build_graph().compile()
