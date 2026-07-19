"""工具基类：所有 MCP 工具继承 BaseTool，run() 统一超时 + 重试 + 降级。"""
from __future__ import annotations

import asyncio
from abc import ABC, abstractmethod
from typing import Any

import structlog
from langchain_core.tools import BaseTool as LangchainBaseTool
from pydantic import BaseModel
from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.config import settings

logger = structlog.get_logger(__name__)


class BaseTool(ABC):
    name: str
    description: str
    args_schema: type[BaseModel]
    intents: list[str] = []

    @abstractmethod
    async def _run(self, **kwargs: Any) -> dict[str, Any]: ...

    @retry(
        stop=stop_after_attempt(2),
        wait=wait_exponential(multiplier=0.5, min=0.5, max=2),
        reraise=True,
    )
    async def run(self, **kwargs: Any) -> dict[str, Any]:
        try:
            return await asyncio.wait_for(self._run(**kwargs), timeout=settings.backend_timeout)
        except TimeoutError:
            logger.warning("tool_timeout", tool=self.name)
            return {"status": "error", "error": "timeout", "tool": self.name}
        except Exception as e:
            logger.warning("tool_error", tool=self.name, error=str(e))
            return {"status": "error", "error": str(e), "tool": self.name}

    def to_langchain(self) -> LangchainBaseTool:
        from langchain_core.tools import StructuredTool

        async def _invoke(**kwargs: Any) -> dict:
            return await self.run(**kwargs)

        return StructuredTool.from_function(
            func=None,
            coroutine=_invoke,
            name=self.name,
            description=self.description,
            args_schema=self.args_schema,
        )
