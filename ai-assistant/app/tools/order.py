"""订单查询工具 (MCP)。"""
from __future__ import annotations

import httpx
from pydantic import BaseModel, Field

from app.core.config import settings
from app.tools.base import BaseTool


class OrderQueryArgs(BaseModel):
    user_id: int = Field(..., description="用户 ID")
    order_no: str | None = Field(default=None, description="订单号 (可选)")


class OrderQueryTool(BaseTool):
    name = "order_query"
    description = "查询用户的订单列表或单个订单详情。用于售后场景。"
    args_schema = OrderQueryArgs
    intents = ["after_sale"]

    async def _run(self, user_id: int, order_no: str | None = None) -> dict:
        async with httpx.AsyncClient(timeout=settings.backend_timeout) as client:
            url = (
                f"{settings.backend_base_url}/orders/{order_no}"
                if order_no
                else f"{settings.backend_base_url}/orders"
            )
            r = await client.get(
                url,
                params={"userId": user_id, "pageSize": 10} if not order_no else None,
                headers={"X-Service-Token": settings.backend_service_token},
            )
            r.raise_for_status()
            return {"status": "ok", "tool": self.name, "data": r.json().get("data")}
