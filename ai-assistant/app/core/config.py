"""集中化配置 (Pydantic Settings)：启动时校验必填项，缺失即 fail-fast。"""
from __future__ import annotations

from functools import lru_cache

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_name: str = "ai-assistant"
    app_env: str = "dev"
    app_port: int = 8000
    log_level: str = "INFO"
    cors_origins: str = "http://localhost:5173,http://localhost:3000"

    openai_api_key: str = Field(..., description="LLM API Key")
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"

    mysql_dsn: str = Field(..., description="MySQL 异步 DSN")

    redis_url: str = "redis://localhost:6379/0"

    milvus_host: str = "localhost"
    milvus_port: int = 19530

    backend_base_url: str = "http://localhost:8080/api"
    backend_service_token: str = Field(..., description="后端服务内部 token")
    backend_timeout: int = 10

    langsmith_tracing: bool = False
    langsmith_api_key: str = ""
    langsmith_project: str = "ecommerce-ai-assistant"

    @field_validator("cors_origins")
    @classmethod
    def _strip(cls, v: str) -> str:
        return v.strip()

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    @property
    def is_production(self) -> bool:
        return self.app_env == "prod"


@lru_cache
def get_settings() -> Settings:
    return Settings()  # type: ignore[call-arg]


settings = get_settings()
