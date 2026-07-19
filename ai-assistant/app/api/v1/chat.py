"""对话 API：SSE 流式输出，支持主动中断 + 后端 HTTP 落库 + JWT 鉴权。"""
from __future__ import annotations

import asyncio
import json
import re
import uuid
from typing import Any

import structlog
from fastapi import APIRouter, Header, HTTPException, Query
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from sse_starlette.sse import EventSourceResponse

from app.agents.graph import get_compiled_graph
from app.memory.long_term import memory as long_term_memory
from app.schemas.chat import ChatRequest, MessageVO, SessionVO
from app.services.backend_client import backend_client

logger = structlog.get_logger(__name__)
router = APIRouter()

_cancel_events: dict[str, asyncio.Event] = {}


# ---------------------------------------------------------------------------
# JWT 用户身份提取
# ---------------------------------------------------------------------------
async def _resolve_user(authorization: str | None = Header(None)) -> int | None:
    """从 Authorization Bearer token 中解析 user_id (sub claim)。"""
    if not authorization or not authorization.startswith("Bearer "):
        return None
    token = authorization[7:]
    try:
        parts = token.split(".")
        if len(parts) != 3:
            return None
        import base64

        payload_b64 = parts[1]
        padding = 4 - len(payload_b64) % 4
        if padding != 4:
            payload_b64 += "=" * padding
        decoded = json.loads(base64.urlsafe_b64decode(payload_b64))
        sub = decoded.get("sub")
        if sub is not None:
            return int(sub)
    except Exception:
        logger.warning("jwt_decode_failed", exc_info=True)
    return None


# ---------------------------------------------------------------------------
# SSE 事件处理
# ---------------------------------------------------------------------------
_GLM_TAG_RE = re.compile(
    r"<tool_call>.*?</tool_call>|<think>.*?</think>|<arg_key>.*?</arg_key>|<arg_value>.*?</arg_value>",
    re.DOTALL,
)


def _clean_content(text: str) -> str:
    if not text:
        return ""
    return _GLM_TAG_RE.sub("", text).strip()


def _sse_chunk(type_: str, content: str = "", **extra) -> str:
    return json.dumps({"type": type_, "content": content, **extra}, ensure_ascii=False)


# ---------------------------------------------------------------------------
# 会话标题
# ---------------------------------------------------------------------------
def _auto_title(user_message: str) -> str:
    return user_message if len(user_message) <= 35 else user_message[:35] + "..."


# ---------------------------------------------------------------------------
# 核心 POST /chat 流式接口
# ---------------------------------------------------------------------------
@router.post("/chat")
async def chat_stream(
    req: ChatRequest,
    authorization: str | None = Header(None),
) -> EventSourceResponse:
    user_id = await _resolve_user(authorization)
    if user_id is None:
        raise HTTPException(status_code=401, detail="Missing or invalid JWT")

    req.user_id = user_id
    session_id = req.session_id or uuid.uuid4().hex
    is_new_session = req.session_id is None

    cancel_event = asyncio.Event()
    _cancel_events[session_id] = cancel_event

    async def event_generator():
        _sent_token = False
        _assistant_content = ""
        _tool_call_records: list[dict[str, Any]] = []

        try:
            graph = get_compiled_graph()

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

            state_in: dict[str, Any] = {
                "messages": [*history_messages, HumanMessage(content=req.message)],
                "session_id": session_id,
                "user_id": user_id,
                "user_profile": {},
                "tool_results": [],
                "retry_count": 0,
                "cancelled": False,
            }

            # 加载用户画像
            try:
                profile = await long_term_memory.get(user_id)
                if profile:
                    state_in["user_profile"] = profile
            except Exception:
                logger.warning("load_profile_failed", user_id=user_id, exc_info=True)

            _sent_ids: set[str] = set()

            async for event in graph.astream(state_in):
                if cancel_event.is_set():
                    yield {"event": "done", "data": '{"cancelled":true}'}
                    break
                for _node_name, node_state in event.items():
                    messages = node_state.get("messages", [])
                    if messages and isinstance(messages[-1], AIMessage):
                        chunk = messages[-1]
                        msg_id = getattr(chunk, "id", None)
                        if msg_id and msg_id in _sent_ids:
                            continue
                        if msg_id:
                            _sent_ids.add(msg_id)

                        raw = chunk.content if isinstance(chunk.content, str) else ""
                        cleaned = _clean_content(raw)

                        if cleaned:
                            _sent_token = True
                            _assistant_content += cleaned
                            yield {
                                "event": "message",
                                "data": _sse_chunk("token", cleaned),
                            }

                        tool_calls = getattr(chunk, "tool_calls", None) or getattr(
                            chunk, "tool_call_chunks", None
                        )
                        if tool_calls:
                            for tc in tool_calls:
                                name = (
                                    tc.get("name")
                                    if isinstance(tc, dict)
                                    else getattr(tc, "name", None)
                                )
                                args = (
                                    tc.get("args")
                                    if isinstance(tc, dict)
                                    else getattr(tc, "args", "{}")
                                )
                                if name:
                                    _tool_call_records.append(
                                        {"name": name, "args": str(args)}
                                    )
                                    yield {
                                        "event": "message",
                                        "data": _sse_chunk(
                                            "tool_call",
                                            name=name,
                                            args=json.dumps(
                                                args, ensure_ascii=False
                                            )
                                            if isinstance(args, dict)
                                            else str(args),
                                        ),
                                    }

                    for tr in node_state.get("tool_results", []):
                        yield {
                            "event": "message",
                            "data": _sse_chunk("tool_result", "", **tr),
                        }

            # 兜底消息
            if not _sent_token:
                fallback = "抱歉，当前助手暂时未能生成回复，请换种说法或稍后重试。"
                _assistant_content = fallback
                yield {
                    "event": "message",
                    "data": _sse_chunk("token", fallback),
                }

            message_id = uuid.uuid4().hex
            yield {
                "event": "message",
                "data": _sse_chunk("done", messageId=message_id),
            }

        except asyncio.CancelledError:
            logger.info("chat_cancelled", session_id=session_id)
            yield {"event": "done", "data": '{"cancelled":true}'}
            return
        except Exception as e:
            logger.error(
                "chat_failed", session_id=session_id, error=str(e), exc_info=True
            )
            err_msg = f"服务异常: {str(e)}"
            _assistant_content = err_msg
            yield {
                "event": "message",
                "data": _sse_chunk("error", error=err_msg),
            }
            yield {
                "event": "message",
                "data": _sse_chunk("done", messageId=uuid.uuid4().hex),
            }
        finally:
            _cancel_events.pop(session_id, None)
            # 后台通过 HTTP 落库到 Java Backend
            asyncio.create_task(
                _persist_conversation(
                    session_id=session_id,
                    user_id=user_id,
                    user_message=req.message,
                    assistant_content=_assistant_content,
                    is_new=is_new_session,
                    tool_records=_tool_call_records,
                )
            )

    async def _on_disconnect(_msg):
        cancel_event.set()
        _cancel_events.pop(session_id, None)

    return EventSourceResponse(
        event_generator(),
        client_close_handler_callable=_on_disconnect,
    )


# ---------------------------------------------------------------------------
# 会话/消息落库（通过 HTTP → Java Backend）
# ---------------------------------------------------------------------------
async def _persist_conversation(
    session_id: str,
    user_id: int,
    user_message: str,
    assistant_content: str,
    is_new: bool,
    tool_records: list[dict[str, Any]],
) -> None:
    """将一轮对话通过 Java Backend API 持久化。"""
    try:
        if is_new:
            title = _auto_title(user_message)
            await backend_client.create_session(session_id, user_id, title)
        else:
            # 尝试更新标题（首次消息若已落库则忽略）
            try:
                existing = await backend_client.get_session(session_id, user_id)
                if existing is None:
                    title = _auto_title(user_message)
                    await backend_client.create_session(session_id, user_id, title)
            except Exception:
                pass

        # user 消息
        user_msg_id = f"um_{uuid.uuid4().hex[:20]}"
        await backend_client.create_message(
            session_id=session_id,
            message_id=user_msg_id,
            role="user",
            content=user_message,
        )

        # assistant 消息
        assistant_msg_id = f"am_{uuid.uuid4().hex[:20]}"
        meta = (
            json.dumps({"tool_calls": tool_records}, ensure_ascii=False)
            if tool_records
            else None
        )
        await backend_client.create_message(
            session_id=session_id,
            message_id=assistant_msg_id,
            role="assistant",
            content=assistant_content,
            metadata_json=meta,
        )

        logger.info(
            "chat_persisted",
            session_id=session_id,
            user_id=user_id,
            msg_len=len(assistant_content),
        )

        # 更新长期记忆
        try:
            await long_term_memory.record_search(user_id, user_message[:100])
        except Exception:
            pass

    except Exception as e:
        logger.error(
            "persist_failed", session_id=session_id, error=str(e), exc_info=True
        )


# ---------------------------------------------------------------------------
# 停止对话
# ---------------------------------------------------------------------------
@router.post("/chat/stop")
async def chat_stop(session_id: str) -> dict[str, Any]:
    event = _cancel_events.get(session_id)
    if not event:
        raise HTTPException(
            status_code=404, detail="Session not found or already finished"
        )
    event.set()
    return {"code": 0, "message": "ok"}


# ---------------------------------------------------------------------------
# 会话列表（代理 → Java Backend）
# ---------------------------------------------------------------------------
@router.get("/chat/sessions")
async def list_sessions(
    authorization: str | None = Header(None),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
) -> dict[str, Any]:
    user_id = await _resolve_user(authorization)
    if user_id is None:
        raise HTTPException(status_code=401, detail="Missing or invalid JWT")

    try:
        data = await backend_client.list_sessions(user_id, limit=limit, offset=offset)
        items = [
            SessionVO(
                id=s.get("id", ""),
                title=s.get("title", ""),
                created_at=s.get("createdAt", ""),
                updated_at=s.get("updatedAt", ""),
                message_count=s.get("messageCount", 0),
            )
            for s in data
        ]
        return {"code": 0, "message": "ok", "data": [i.model_dump() for i in items]}
    except Exception as e:
        logger.error("list_sessions_failed", error=str(e))
        return {"code": 0, "message": "ok", "data": []}


# ---------------------------------------------------------------------------
# 会话详情（代理 → Java Backend）
# ---------------------------------------------------------------------------
@router.get("/chat/sessions/{session_id}")
async def get_session_detail(
    session_id: str,
    authorization: str | None = Header(None),
) -> dict[str, Any]:
    user_id = await _resolve_user(authorization)
    if user_id is None:
        raise HTTPException(status_code=401, detail="Missing or invalid JWT")

    try:
        data = await backend_client.get_session(session_id, user_id)
        if data is None:
            raise HTTPException(status_code=404, detail="Session not found")

        session_data = data.get("session", data)
        messages_data = data.get("messages", [])

        session_vo = SessionVO(
            id=session_data.get("id", ""),
            title=session_data.get("title", ""),
            created_at=session_data.get("createdAt", ""),
            updated_at=session_data.get("updatedAt", ""),
            message_count=session_data.get("messageCount", len(messages_data)),
        )
        msg_vos = [
            MessageVO(
                id=m.get("id", ""),
                role=m.get("role", ""),
                content=m.get("content", ""),
                metadata=m.get("metadata"),
                created_at=m.get("createdAt", ""),
            )
            for m in messages_data
        ]
        return {
            "code": 0,
            "message": "ok",
            "data": {
                "session": session_vo.model_dump(),
                "messages": [m.model_dump() for m in msg_vos],
            },
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error("get_session_failed", session_id=session_id, error=str(e))
        raise HTTPException(status_code=500, detail="Failed to fetch session")


# ---------------------------------------------------------------------------
# 删除会话（代理 → Java Backend）
# ---------------------------------------------------------------------------
@router.delete("/chat/sessions/{session_id}")
async def delete_session(
    session_id: str,
    authorization: str | None = Header(None),
) -> dict[str, Any]:
    user_id = await _resolve_user(authorization)
    if user_id is None:
        raise HTTPException(status_code=401, detail="Missing or invalid JWT")

    ok = await backend_client.delete_session(session_id, user_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Session not found")
    return {"code": 0, "message": "ok"}
