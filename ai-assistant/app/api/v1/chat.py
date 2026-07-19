"""对话 API：SSE 流式输出，支持主动中断。"""
from __future__ import annotations

import asyncio
import json
import uuid
from typing import Any

import structlog
from fastapi import APIRouter, HTTPException
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage, SystemMessage
from sse_starlette.sse import EventSourceResponse

from app.agents.graph import get_compiled_graph
from app.schemas.chat import ChatRequest

logger = structlog.get_logger(__name__)
router = APIRouter()

_cancel_events: dict[str, asyncio.Event] = {}


@router.post("/chat")
async def chat_stream(req: ChatRequest) -> EventSourceResponse:
    session_id = req.session_id or uuid.uuid4().hex
    cancel_event = asyncio.Event()
    _cancel_events[session_id] = cancel_event

    async def event_generator():
        try:
            graph = get_compiled_graph()

            # 构建消息历史：将前端传来的 history 转换为 LangChain Message，但跳过 tool 类型
            history_messages: list = []
            if req.history:
                role_map = {
                    "user": HumanMessage,
                    "assistant": AIMessage,
                    "system": SystemMessage,
                }
                for h in req.history:
                    cls = role_map.get(h.role)
                    if cls and h.content:
                        history_messages.append(cls(content=h.content))

            state_in = {
                "messages": [*history_messages, HumanMessage(content=req.message)],
                "session_id": session_id,
                "tool_results": [],
                "retry_count": 0,
                "cancelled": False,
            }

            async for event in graph.astream(state_in):
                if cancel_event.is_set():
                    yield {"event": "done", "data": '{"cancelled":true}'}
                    break
                for _node_name, node_state in event.items():
                    messages = node_state.get("messages", [])
                    if messages and isinstance(messages[-1], AIMessageChunk):
                        chunk = messages[-1]
                        if chunk.content:
                            yield {"event": "message", "data": _sse_chunk("token", chunk.content)}
                        # 发出 tool_call 事件（前端用于展示工具调用提示）
                        if hasattr(chunk, "tool_call_chunks") and chunk.tool_call_chunks:
                            for tcc in chunk.tool_call_chunks:
                                if tcc.name:
                                    yield {
                                        "event": "message",
                                        "data": _sse_chunk("tool_call", name=tcc.name, args=tcc.args or "{}"),
                                    }
                    for tr in node_state.get("tool_results", []):
                        yield {"event": "message", "data": _sse_chunk("tool_result", "", **tr)}

            yield {"event": "message", "data": _sse_chunk("done", messageId=session_id)}
        except asyncio.CancelledError:
            logger.info("chat_cancelled", session_id=session_id)
            yield {"event": "done", "data": '{"cancelled":true}'}
        except Exception as e:
            logger.error("chat_failed", session_id=session_id, error=str(e), exc_info=True)
            yield {"event": "message", "data": _sse_chunk("error", error=str(e))}
        finally:
            _cancel_events.pop(session_id, None)

    response = EventSourceResponse(event_generator())

    # 确保客户端断开时清理资源
    @response.on_disconnect
    async def _on_disconnect():
        cancel_event.set()
        _cancel_events.pop(session_id, None)

    return response


@router.post("/chat/stop")
async def chat_stop(session_id: str) -> dict[str, Any]:
    event = _cancel_events.get(session_id)
    if not event:
        raise HTTPException(status_code=404, detail="Session not found or already finished")
    event.set()
    return {"code": 0, "message": "ok"}


@router.get("/chat/sessions")
async def list_sessions() -> dict[str, Any]:
    return {"code": 0, "message": "ok", "data": []}


def _sse_chunk(type_: str, content: str = "", **extra) -> str:
    return json.dumps({"type": type_, "content": content, **extra}, ensure_ascii=False)
