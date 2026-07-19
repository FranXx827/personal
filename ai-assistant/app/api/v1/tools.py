"""MCP 工具端点：供内部服务通过 X-Service-Token 鉴权调用。"""
from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.core.security import verify_service_token
from app.llm.client import get_llm
from app.llm.prompts.tag_generator import TAG_GENERATOR_PROMPT
from app.schemas.chat import ToolInvokeRequest
from app.tools.registry import tool_registry
from langchain_core.messages import HumanMessage, SystemMessage

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


class GenerateTagsRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=512, description="商品标题")
    description: str | None = Field(None, max_length=2048, description="商品描述")


@router.post("/tools/generate-tags", dependencies=[Depends(verify_service_token)])
async def generate_tags(req: GenerateTagsRequest) -> dict[str, Any]:
    """用 LLM 根据商品标题和描述生成搜索标签。"""
    llm = get_llm(temperature=0.3)
    prompt = TAG_GENERATOR_PROMPT.format(
        title=req.title,
        description=req.description or "",
    )
    resp = await llm.ainvoke([
        SystemMessage(content="你是一个电商搜索标签生成器，只输出逗号分隔的标签文本。"),
        HumanMessage(content=prompt),
    ])
    tags = resp.content.strip() if isinstance(resp.content, str) else ""
    # 清理可能的引号和多余空格
    tags = tags.strip("\"'「」").strip()
    return {"code": 0, "message": "ok", "data": {"tags": tags}}
