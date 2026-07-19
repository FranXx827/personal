"""LLM 客户端统一封装：业务代码只通过本模块获取 ChatOpenAI 实例。"""
from __future__ import annotations

from functools import lru_cache

from langchain_openai import ChatOpenAI

from app.core.config import settings


@lru_cache
def _base_llm() -> ChatOpenAI:
    return ChatOpenAI(
        api_key=settings.openai_api_key,
        base_url=settings.openai_base_url,
        model=settings.openai_model,
        temperature=0.7,
        timeout=30,
        max_retries=2,
    )


def get_llm(temperature: float | None = None, model: str | None = None) -> ChatOpenAI:
    llm = _base_llm()
    overrides: dict[str, float | str] = {}
    if temperature is not None:
        overrides["temperature"] = temperature
    if model is not None:
        overrides["model"] = model
    return llm.bind(**overrides) if overrides else llm
