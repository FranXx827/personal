"""ReAct 自愈闭环测试：工具失败 → reflect 触发重试 → 成功 / 降级。"""
from __future__ import annotations

from langchain_core.messages import AIMessage, HumanMessage

from app.agents.nodes import executor
from app.agents.nodes.reflect import reflect_node, route_after_reflect
from app.agents.state import AgentState
from app.tools.base import BaseTool
from app.tools.registry import tool_registry


class _FakeLLM:
    """按脚本返回 AIMessage 的假模型，bind_tools/bind 均返回自身。"""

    def __init__(self, queue):
        self._queue = list(queue)
        self._tools = None

    def bind_tools(self, tools):
        self._tools = tools
        return self

    def bind(self, **kwargs):
        return self

    async def ainvoke(self, messages, *args, **kwargs):
        if not self._queue:
            raise AssertionError("unexpected ainvoke call: queue empty")
        return self._queue.pop(0)


class _FlakyTool(BaseTool):
    name = "fake"
    description = "flaky"
    args_schema = None  # type: ignore
    intents = ["consult"]

    def __init__(self):
        self.calls = 0

    async def _run(self, **kwargs):
        self.calls += 1
        if self.calls == 1:
            return {"status": "error", "error": "boom", "tool": "fake"}
        return {"status": "ok", "data": kwargs}


def _tool_call_msg(name: str, args: dict, cid: str = "call_1") -> AIMessage:
    return AIMessage(
        content="",
        tool_calls=[{"name": name, "args": args, "id": cid, "type": "tool_call"}],
    )


def test_route_branches():
    assert route_after_reflect({"tool_results": [], "retry_count": 0}) == "end"
    assert (
        route_after_reflect({"tool_results": [{"status": "error"}], "retry_count": 0})
        == "reflect"
    )
    assert (
        route_after_reflect({"tool_results": [{"status": "error"}], "retry_count": 2})
        == "degrade"
    )


async def test_run_tools_then_retry_success(monkeypatch):
    tool = _FlakyTool()
    tool_registry.register(tool)

    queue = [
        _tool_call_msg("fake", {"q": "x"}),
        AIMessage(content="初步回答"),
        AIMessage(content='{"rewritten_args": {"q": "retry"}}'),
        AIMessage(content="重试后的最终回答"),
    ]
    monkeypatch.setattr(executor, "get_llm", lambda *a, **k: _FakeLLM(queue))

    state: AgentState = {
        "messages": [HumanMessage(content="推荐个商品")],
        "intent": "consult",
        "tool_results": [],
        "retry_count": 0,
    }

    # 业务节点执行工具，首次失败
    state = await executor.run_tools(state, [tool])
    assert state["tool_results"][0]["status"] == "error"

    # reflect 触发 ReAct 重试，工具二次调用成功
    state = await reflect_node(state)
    assert state["retry_count"] == 1
    assert state["tool_results"][0]["status"] == "ok"
    assert tool.calls == 2

    # 不再失败 → 结束
    assert route_after_reflect(state) == "end"


async def test_run_tools_then_degrade(monkeypatch):
    # 始终失败的工具
    class _AlwaysFail(BaseTool):
        name = "fake"
        description = "always fail"
        args_schema = None  # type: ignore
        intents = ["consult"]

        async def _run(self, **kwargs):
            return {"status": "error", "error": "nope", "tool": "fake"}

    tool = _AlwaysFail()
    tool_registry.register(tool)

    queue = [
        _tool_call_msg("fake", {"q": "x"}),
        AIMessage(content="初步回答"),
        AIMessage(content='{"rewritten_args": {"q": "retry1"}}'),
        AIMessage(content="重试1回答"),
        AIMessage(content='{"rewritten_args": {"q": "retry2"}}'),
        AIMessage(content="重试2回答"),
    ]
    monkeypatch.setattr(executor, "get_llm", lambda *a, **k: _FakeLLM(queue))

    state: AgentState = {
        "messages": [HumanMessage(content="推荐个商品")],
        "intent": "consult",
        "tool_results": [],
        "retry_count": 0,
    }

    state = await executor.run_tools(state, [tool])
    assert state["tool_results"][0]["status"] == "error"

    # 第一次重试
    state = await reflect_node(state)
    assert route_after_reflect(state) == "reflect"
    # 第二次重试后耗尽 → 降级
    state = await reflect_node(state)
    assert route_after_reflect(state) == "degrade"
    assert state["retry_count"] == 2
