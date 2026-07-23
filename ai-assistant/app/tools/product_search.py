"""商品搜索工具（LLM → 品类名称 → 后端映射）"""
from __future__ import annotations

import httpx
from pydantic import BaseModel, Field

from app.core.config import settings
from app.tools.base import BaseTool


class ProductSearchArgs(BaseModel):
    category_name: str = Field(
        ...,
        min_length=1,
        max_length=64,
        description="用户想搜的商品品类名称，如手机、电脑、护肤品、运动鞋等。只传中文名称，不要传ID。",
    )
    limit: int = Field(default=10, ge=1, le=50)


class ProductSearchTool(BaseTool):
    """
    LLM → 品类名称 → 后端分类映射服务 → ID 列表 → 商品搜索。

    LLM 只需输出品类的中文名称（如"手机"、"降噪耳机"），
    后端 CategoryService 负责将名称映射为分类 ID 列表（含子分类），
    使用 IN 查询返回该品类下所有商品。
    """

    name = "search_products"
    description = "按品类搜索商品，例如用户说「推荐手机」时传 category_name='手机'。只传品类中文名称，由系统自动映射。"
    args_schema = ProductSearchArgs
    intents = ["consult"]

    async def _run(self, category_name: str, limit: int = 10) -> dict:
        async with httpx.AsyncClient(timeout=settings.backend_timeout) as client:
            r = await client.get(
                f"{settings.backend_base_url}/products",
                params={"categoryName": category_name, "pageSize": limit},
                headers={"X-Service-Token": settings.backend_service_token},
            )
            r.raise_for_status()
            body = r.json()
            return {
                "status": "ok",
                "tool": self.name,
                "data": body.get("data", {}).get("list", []),
            }
