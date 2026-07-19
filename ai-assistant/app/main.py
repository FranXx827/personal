"""FastAPI 应用入口。"""
from __future__ import annotations

from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1 import chat as chat_v1
from app.api.v1 import health as health_v1
from app.api.v1 import tools as tools_v1
from app.core.config import settings
from app.core.logging import setup_logging
from app.middleware.error import register_error_handlers
from app.middleware.trace import TraceIdMiddleware

logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging(settings.log_level)
    logger.info(
        "ai-assistant starting",
        env=settings.app_env,
        port=settings.app_port,
        llm_model=settings.openai_model,
    )
    yield
    logger.info("ai-assistant shutting down")


def create_app() -> FastAPI:
    app = FastAPI(
        title="智能电商导购 AI 助手",
        version="1.0.0",
        description="基于 LangGraph 多 Agent 协同的对话式电商导购服务",
        lifespan=lifespan,
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
        expose_headers=["X-Trace-Id"],
    )

    app.add_middleware(TraceIdMiddleware)

    register_error_handlers(app)

    app.include_router(health_v1.router, prefix="/api/v1", tags=["健康"])
    app.include_router(chat_v1.router, prefix="/api/v1", tags=["对话"])
    app.include_router(tools_v1.router, prefix="/api/v1", tags=["MCP 工具"])

    return app


app = create_app()
