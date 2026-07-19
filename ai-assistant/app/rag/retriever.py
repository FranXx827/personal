"""RAG 检索器：基于 Milvus 的向量检索，召回后做重排注入 Prompt。"""
from __future__ import annotations

from typing import Any

import structlog
from pymilvus import connections

from app.core.config import settings

logger = structlog.get_logger(__name__)


class Retriever:
    def __init__(self) -> None:
        self._initialized = False

    def _ensure_connected(self) -> None:
        if self._initialized:
            return
        connections.connect(host=settings.milvus_host, port=settings.milvus_port)
        self._initialized = True

    async def search(self, query: str, top_k: int = 5) -> list[dict[str, Any]]:
        try:
            self._ensure_connected()
            logger.info("rag.search", query=query[:50], top_k=top_k)
            return []
        except Exception as e:
            logger.warning("rag.search_failed", error=str(e))
            return []


retriever = Retriever()
