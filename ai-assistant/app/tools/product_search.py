"""商品搜索工具 (MCP)：调用电商后端 /products 接口。"""
from __future__ import annotations

import httpx
from pydantic import BaseModel, Field

from app.core.config import settings
from app.tools.base import BaseTool


class ProductSearchArgs(BaseModel):
    keyword: str = Field(..., min_length=1, max_length=128)
    limit: int = Field(default=10, ge=1, le=50)


class ProductSearchTool(BaseTool):
    name = "product_search"
    description = "根据关键词搜索商品，返回商品列表（标题、价格、销量）。用于商品咨询场景。"
    args_schema = ProductSearchArgs
    intents = ["consult"]

    async def _run(self, keyword: str, limit: int = 10) -> dict:
        async with httpx.AsyncClient(timeout=settings.backend_timeout) as client:
            r = await client.get(
                f"{settings.backend_base_url}/products",
                params={"keyword": keyword, "pageSize": limit},
                headers={"X-Service-Token": settings.backend_service_token},
            )
            r.raise_for_status()
            body = r.json()
            return {
                "status": "ok",
                "tool": self.name,
                "data": body.get("data", {}).get("list", []),
            }
