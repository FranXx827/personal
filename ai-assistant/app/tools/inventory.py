"""库存查询工具 (MCP)。"""
from __future__ import annotations

import httpx
from pydantic import BaseModel, Field

from app.core.config import settings
from app.tools.base import BaseTool


class InventoryArgs(BaseModel):
    sku_id: int = Field(..., description="SKU ID")


class InventoryTool(BaseTool):
    name = "inventory_check"
    description = "查询指定 SKU 的库存数量。用于商品咨询 / 加购前判断。"
    args_schema = InventoryArgs
    intents = ["consult", "cart"]

    async def _run(self, sku_id: int) -> dict:
        async with httpx.AsyncClient(timeout=settings.backend_timeout) as client:
            r = await client.get(
                f"{settings.backend_base_url}/products/sku/{sku_id}/stock",
                headers={"X-Service-Token": settings.backend_service_token},
            )
            r.raise_for_status()
            return {"status": "ok", "tool": self.name, "data": r.json().get("data")}
