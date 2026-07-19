"""MCP 工具端点：供内部服务通过 X-Service-Token 鉴权调用。"""
from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends

from app.core.security import verify_service_token
from app.schemas.chat import ToolInvokeRequest
from app.tools.registry import tool_registry

router = APIRouter()


@router.post("/tools/invoke", dependencies=[Depends(verify_service_token)])
async def invoke_tool(req: ToolInvokeRequest) -> dict[str, Any]:
    tool = tool_registry.get(req.name)
    if tool is None:
        return {"code": 10002, "message": f"Tool {req.name!r} not found", "data": None}
    result = await tool.run(**req.arguments)
    return {"code": 0, "message": "ok", "data": result}


@router.get("/tools", dependencies=[Depends(verify_service_token)])
async def list_tools() -> dict[str, Any]:
    return {
        "code": 0,
        "message": "ok",
        "data": [
            {
                "name": t.name,
                "description": t.description,
                "intents": t.intents,
                "args_schema": t.args_schema.model_json_schema(),
            }
            for t in tool_registry.all()
        ],
    }
