"""
测试 fixtures
"""
from __future__ import annotations

import pytest

from app.tools.base import BaseTool
from app.tools.registry import tool_registry


@pytest.fixture(autouse=True)
def _clean_registry():
    """每个测试前后清理工具注册表"""
    saved = dict(tool_registry._tools)
    tool_registry._tools.clear()
    yield
    tool_registry._tools.clear()
    tool_registry._tools.update(saved)


@pytest.fixture
def fake_tool() -> BaseTool:
    class _T(BaseTool):
        name = "fake"
        description = "test tool"
        args_schema = None  # type: ignore
        intents = ["consult"]

        async def _run(self, **kwargs):
            return {"status": "ok", "data": kwargs}

    return _T()
