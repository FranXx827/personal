"""请求链路追踪：从 X-Trace-Id 读取或生成，注入日志上下文并回写响应头。"""
from __future__ import annotations

import uuid

import structlog
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

HEADER = "X-Trace-Id"


class TraceIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        trace_id = request.headers.get(HEADER) or uuid.uuid4().hex
        structlog.contextvars.bind_contextvars(trace_id=trace_id)

        response = await call_next(request)
        response.headers[HEADER] = trace_id

        structlog.contextvars.clear_contextvars()
        return response
