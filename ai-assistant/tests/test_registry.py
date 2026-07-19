"""工具注册测试"""
import pytest

from app.tools.registry import tool_registry


def test_register_and_get(fake_tool):
    tool_registry.register(fake_tool)
    assert tool_registry.get("fake") is fake_tool


def test_list_for_intent(fake_tool):
    tool_registry.register(fake_tool)
    tools = tool_registry.list_for_intent("consult")
    assert tools == [fake_tool]
    assert tool_registry.list_for_intent("after_sale") == []


def test_duplicate_register_raises(fake_tool):
    tool_registry.register(fake_tool)
    try:
        tool_registry.register(fake_tool)
        pytest.fail("should have raised")
    except ValueError:
        pass
