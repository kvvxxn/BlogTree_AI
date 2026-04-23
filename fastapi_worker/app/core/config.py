from __future__ import annotations

import os
from enum import StrEnum
from pathlib import Path
from urllib.parse import quote

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


WORKER_ROOT = Path(__file__).resolve().parents[2]


class AppEnv(StrEnum):
    DEV = "dev"
    PROD = "prod"


def _select_env_file() -> Path | None:
    override = os.getenv("APP_ENV_FILE")
    if override:
        override_path = Path(override)
        if not override_path.is_absolute():
            override_path = WORKER_ROOT / override_path
        return override_path

    raw_env = os.getenv("APP_ENV", AppEnv.DEV.value).lower()
    app_env = AppEnv(raw_env)

    env_file = WORKER_ROOT / f".env.{app_env.value}"
    if env_file.exists():
        return env_file

    fallback = WORKER_ROOT / ".env"
    if fallback.exists():
        return fallback

    return None


class Settings(BaseSettings):
    APP_ENV: AppEnv = AppEnv.DEV

    # Required in every environment.
    LLM_API_KEY: str | None = None

    # Optional integrations.
    LANGFUSE_PUBLIC_KEY: str | None = None
    LANGFUSE_SECRET_KEY: str | None = None
    LANGFUSE_HOST: str | None = None
    LANGFUSE_BASE_URL: str | None = None

    # RabbitMQ.
    # - dev: if omitted, localhost defaults are used.
    # - prod: define either RABBITMQ_URL or the granular fields below.
    RABBITMQ_URL: str | None = None
    RABBITMQ_HOST: str | None = None
    RABBITMQ_PORT: int | None = None
    RABBITMQ_USER: str | None = None
    RABBITMQ_PASSWORD: str | None = None
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_SSL: bool | None = None

    # Messaging topology defaults.
    SUMMARY_EXCHANGE: str = "summary.exchange"
    RECOMMEND_EXCHANGE: str = "recommend.exchange"
    SUMMARIZE_INPUT_QUEUE: str = "summary.request.queue"
    SUMMARIZE_OUTPUT_QUEUE: str = "summary.response.queue"
    RECOMMEND_INPUT_QUEUE: str = "recommend.request.queue"
    RECOMMEND_OUTPUT_QUEUE: str = "recommend.response.queue"
    SUMMARIZE_INPUT_ROUTING_KEY: str = "summary.request"
    SUMMARIZE_OUTPUT_ROUTING_KEY: str = "summary.response"
    RECOMMEND_INPUT_ROUTING_KEY: str = "recommend.request"
    RECOMMEND_OUTPUT_ROUTING_KEY: str = "recommend.response"

    model_config = SettingsConfigDict(
        env_file_encoding="utf-8",
        env_ignore_empty=True,
        extra="ignore",
    )

    @property
    def is_dev(self) -> bool:
        return self.APP_ENV == AppEnv.DEV

    @property
    def rabbitmq_host(self) -> str | None:
        if self.RABBITMQ_HOST:
            return self.RABBITMQ_HOST
        if self.is_dev:
            return "localhost"
        return None

    @property
    def rabbitmq_port(self) -> int:
        if self.RABBITMQ_PORT is not None:
            return self.RABBITMQ_PORT
        return 5672 if self.is_dev else 5671

    @property
    def rabbitmq_user(self) -> str | None:
        if self.RABBITMQ_USER:
            return self.RABBITMQ_USER
        if self.is_dev:
            return "rabbitmq_user"
        return None

    @property
    def rabbitmq_password(self) -> str | None:
        if self.RABBITMQ_PASSWORD:
            return self.RABBITMQ_PASSWORD
        if self.is_dev:
            return "rabbitmq_password"
        return None

    @property
    def rabbitmq_ssl(self) -> bool:
        if self.RABBITMQ_SSL is not None:
            return self.RABBITMQ_SSL
        return not self.is_dev

    @property
    def rabbitmq_url(self) -> str:
        if self.RABBITMQ_URL:
            return self.RABBITMQ_URL

        host = self.rabbitmq_host
        user = self.rabbitmq_user
        password = self.rabbitmq_password
        if not host or not user or not password:
            raise RuntimeError("RabbitMQ settings are incomplete.")

        scheme = "amqps" if self.rabbitmq_ssl else "amqp"
        encoded_vhost = quote(self.RABBITMQ_VHOST, safe="")
        return f"{scheme}://{user}:{password}@{host}:{self.rabbitmq_port}/{encoded_vhost}"

    @model_validator(mode="after")
    def validate_required_settings(self) -> "Settings":
        missing: list[str] = []

        if not self.LLM_API_KEY:
            missing.append("LLM_API_KEY")

        if not self.RABBITMQ_URL:
            if self.rabbitmq_host is None:
                missing.append("RABBITMQ_HOST")
            if self.rabbitmq_user is None:
                missing.append("RABBITMQ_USER")
            if self.rabbitmq_password is None:
                missing.append("RABBITMQ_PASSWORD")

        if missing:
            joined = ", ".join(missing)
            raise ValueError(f"Missing required environment values: {joined}")

        return self


settings = Settings(_env_file=_select_env_file())
