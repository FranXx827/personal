"""验证与 glm-4.5-air 的对话连通性。读取 .env 中的智谱凭据，直接调用 OpenAI 兼容接口。"""
import os
import sys
import time
import json
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parent.parent


def load_env() -> dict:
    env: dict[str, str] = {}
    p = ROOT / ".env"
    if p.exists():
        for line in p.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    return env


def main() -> int:
    env = load_env()
    api_key = env.get("ZHIPUAI_API_KEY")
    model = env.get("ZHIPUAI_MODEL", "glm-4.5-air")
    base_url = env.get("ZHIPUAI_BASE_URL", "https://open.bigmodel.cn/api/paas/v4/").rstrip("/")

    if not api_key:
        print("❌ 未找到 ZHIPUAI_API_KEY，无法发起请求")
        return 1

    print(f"模型: {model}")
    print(f"接口: {base_url}/chat/completions")
    print("-" * 50)

    url = f"{base_url}/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": "你是一个简洁友好的助手，用中文回答。"},
            {"role": "user", "content": "你好，请用一句话证明你能正常对话，并说出现在的年份。"},
        ],
        "temperature": 0.7,
        "max_tokens": 200,
    }

    t0 = time.time()
    try:
        with httpx.Client(timeout=30) as client:
            resp = client.post(url, headers=headers, json=payload)
    except Exception as e:  # noqa: BLE001
        print(f"❌ 请求失败（网络/超时）：{e}")
        return 2

    elapsed = time.time() - t0
    print(f"HTTP 状态: {resp.status_code}   耗时: {elapsed:.2f}s")
    print("-" * 50)

    if resp.status_code != 200:
        print("❌ 接口返回非 200")
        print(resp.text[:2000])
        return 3

    try:
        data = resp.json()
    except Exception as e:  # noqa: BLE001
        print(f"❌ 响应不是合法 JSON：{e}")
        print(resp.text[:2000])
        return 4

    try:
        content = data["choices"][0]["message"]["content"]
        usage = data.get("usage", {})
    except (KeyError, IndexError) as e:
        print(f"❌ 响应结构异常：{e}")
        print(json.dumps(data, ensure_ascii=False, indent=2)[:2000])
        return 5

    print("✅ 收到模型回复：")
    print(content)
    print("-" * 50)
    print(f"Token 用量: prompt={usage.get('prompt_tokens')} completion={usage.get('completion_tokens')} total={usage.get('total_tokens')}")
    print("\n结论：glm-4.5-air 对话正常 ✅")
    return 0


if __name__ == "__main__":
    sys.exit(main())
