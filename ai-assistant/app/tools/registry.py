"""工具注册中心：启动时注册内置工具，业务节点按意图查询。"""
from __future__ import annotations

from collections.abc import Iterable

from app.tools.base import BaseTool


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, BaseTool] = {}

    def register(self, tool: BaseTool) -> None:
        if tool.name in self._tools:
            raise ValueError(f"Tool {tool.name!r} already registered")
        self._tools[tool.name] = tool

    def register_many(self, tools: Iterable[BaseTool]) -> None:
        for t in tools:
            self.register(t)

    def get(self, name: str) -> BaseTool | None:
        return self._tools.get(name)

    def list_for_intent(self, intent: str) -> list[BaseTool]:
        return [t for t in self._tools.values() if intent in t.intents]

    def all(self) -> list[BaseTool]:
        return list(self._tools.values())


tool_registry = ToolRegistry()


def bootstrap_tools() -> None:
    from app.tools.inventory import InventoryTool
    from app.tools.order import OrderQueryTool
    from app.tools.product_search import ProductSearchTool

    tool_registry.register_many([ProductSearchTool(), InventoryTool(), OrderQueryTool()])
