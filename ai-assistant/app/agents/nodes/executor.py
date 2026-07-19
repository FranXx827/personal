"""意图节点的工具执行与 ReAct 重试逻辑（被 consult/cart/after_sale 与 reflect 共用）。"""
from __future__ import annotations

import json
from typing import Any

from langchain_core.messages import AIMessage, ToolMessage

from app.agents.state import AgentState
from app.llm.client import get_llm
from app.tools.base import BaseTool
from app.tools.registry import tool_registry

_MAX_TOOL_ROUNDS = 3


def _result_to_content(result: dict[str, Any]) -> str:
    try:
        return json.dumps(result, ensure_ascii=False)
    except TypeError:
        return str(result)


def _extract_json(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        text = text.strip("`")
    start, end = text.find("{"), text.rfind("}")
    return text[start : end + 1] if start != -1 and end > start else text


async def run_tools(state: AgentState, tools: list[BaseTool]) -> AgentState:
    """调用 LLM 并执行其工具调用，记录每次结果，最后用工具结果综合作答。"""
    model = get_llm()
    messages = list(state["messages"])
    tool_results: list[dict[str, Any]] = list(state.get("tool_results", []))
    if not tools:
        return {**state, "messages": messages, "tool_results": tool_results, "retry_count": 0}

    model_with_tools = model.bind_tools([t.to_langchain() for t in tools])
    tool_map = {t.name: t for t in tools}

    for _ in range(_MAX_TOOL_ROUNDS):
        response = await model_with_tools.ainvoke(messages)
        messages.append(response)
        if not (isinstance(response, AIMessage) and response.tool_calls):
            break
        for tc in response.tool_calls:
            name = tc["name"]
            args = tc.get("args") or {}
            tool = tool_map.get(name)
            result = (
                await tool.run(**args)
                if tool is not None
                else {"status": "error", "error": f"unknown tool: {name}", "tool": name}
            )
            record = {**result, "tool_call_id": tc["id"], "name": name, "args": args}
            tool_results.append(record)
            messages.append(
                ToolMessage(content=_result_to_content(result), tool_call_id=tc["id"], name=name)
            )

    # 末轮若仍是工具调用响应，补一次无工具的总结
    if not isinstance(messages[-1], AIMessage) or getattr(messages[-1], "tool_calls", None):
        messages.append(await model.ainvoke(messages))

    return {**state, "messages": messages, "tool_results": tool_results, "retry_count": 0}


async def retry_failed(state: AgentState) -> AgentState:
    """对最近失败的工具做 ReAct 反思、重写参数并重跑，再综合生成新回答。"""
    messages = list(state["messages"])
    tool_results: list[dict[str, Any]] = list(state.get("tool_results", []))
    tools = {t.name: t for t in tool_registry.list_for_intent(state.get("intent", "unknown"))}
    failed = [r for r in tool_results[-3:] if r.get("status") == "error"]

    model = get_llm(temperature=0.3)
    for r in failed:
        tool = tools.get(r.get("name", ""))
        if tool is None:
            continue
        corrected = await _rewrite_args(model, r)
        new_result = await tool.run(**corrected)
        record = {**new_result, "tool_call_id": r.get("tool_call_id"), "name": r.get("name"), "args": corrected}
        tcid = r.get("tool_call_id")
        for i, m in enumerate(messages):
            if isinstance(m, ToolMessage) and m.tool_call_id == tcid:
                messages[i] = ToolMessage(content=_result_to_content(new_result), tool_call_id=tcid, name=r.get("name"))
                break
        for j, rec in enumerate(tool_results):
            if rec.get("tool_call_id") == tcid:
                tool_results[j] = record
                break

    messages.append(await model.ainvoke(messages))
    return {**state, "messages": messages, "tool_results": tool_results, "retry_count": state.get("retry_count", 0) + 1}


async def _rewrite_args(model, failed_record: dict[str, Any]) -> dict[str, Any]:
    """让 LLM 根据错误重写工具参数；解析失败则回退原参数。"""
    prompt = (
        "工具调用失败，请分析错误并重写参数。\n"
        f"工具: {failed_record.get('name')}\n"
        f"原参数: {json.dumps(failed_record.get('args', {}), ensure_ascii=False)}\n"
        f"错误: {failed_record.get('error')}\n"
        '仅输出 JSON：{"rewritten_args": {...}}'
    )
    try:
        resp = await model.ainvoke(prompt)
        text = resp.content if isinstance(resp, AIMessage) else str(resp)
        data = json.loads(_extract_json(text))
        if isinstance(data.get("rewritten_args"), dict):
            return data["rewritten_args"]
    except Exception:
        pass
    return failed_record.get("args", {})
