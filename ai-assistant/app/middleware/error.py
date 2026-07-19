"""全局错误处理。"""
from __future__ import annotations

import structlog
from fastapi import FastAPI, Request
from fastapi.responses import ORJSONResponse

logger = structlog.get_logger(__name__)


class AppError(Exception):
    def __init__(self, code: int, message: str, status_code: int = 400):
        self.code = code
        self.message = message
        self.status_code = status_code
        super().__init__(message)


class ToolCallError(AppError):
    def __init__(self, message: str):
        super().__init__(code=10002, message=message, status_code=502)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def app_error_handler(_: Request, exc: AppError) -> ORJSONResponse:
        logger.warning("app_error", code=exc.code, message=exc.message)
        return ORJSONResponse(
            status_code=exc.status_code,
            content={"code": exc.code, "message": exc.message, "data": None},
        )

    @app.exception_handler(Exception)
    async def unhandled(_: Request, exc: Exception) -> ORJSONResponse:
        logger.error("unhandled_exception", exc_info=exc)
        return ORJSONResponse(
            status_code=500,
            content={"code": 99999, "message": "系统异常", "data": None},
        )
