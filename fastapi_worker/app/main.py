import os
import asyncio
import logging
import signal
from contextlib import asynccontextmanager
from langfuse import get_client

from fastapi import FastAPI

from fastapi_worker.app.api.health import router as health_router
from fastapi_worker.app.core.config import settings
from fastapi_worker.app.mq.recommend_consumer import start_consuming as start_recommend_consumer
from fastapi_worker.app.mq.summarize_consumer import start_consuming as start_summarize_consumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(filename)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

logger = logging.getLogger(__name__)

def _mask_secret(value: str | None) -> str:
    """
    Secret value를 마스킹하는 함수

    params:
    - value: 마스킹할 문자열 

    return: 마스킹된 문자열
    - value이 None이거나 빈 문자열인 경우 "MISSING" 반환
    """
    if not value:
        return "MISSING"
    if len(value) <= 10:
        return value[:2] + "***"
    return value[:6] + "..." + value[-4:]

def _debug_langfuse() -> None:
    """
    Langfuse 연결 상태 및 연결 정보를 로그로 출력하는 함수

    params: None

    return: None
    """
    logger.info(
        "[Langfuse Debug] PUBLIC=%s SECRET=%s HOST=%s BASE_URL=%s",
        _mask_secret(settings.LANGFUSE_PUBLIC_KEY),
        _mask_secret(settings.LANGFUSE_SECRET_KEY),
        settings.LANGFUSE_HOST or "MISSING",
        settings.LANGFUSE_BASE_URL or "MISSING",
    )

    try:
        langfuse = get_client()
        logger.info("[Langfuse Debug] auth_check=%s", langfuse.auth_check())
    except Exception:
        logger.exception("[Langfuse Debug] auth_check failed")


async def _shutdown_langfuse() -> None:
    """
    Langfuse client를 비동기로 안전하게 종료하는 함수

    params: None

    return: None
    """
    try:
        langfuse = get_client()
    except Exception:
        logger.exception("Langfuse client initialization failed during shutdown")
        return

    try:
        flush = getattr(langfuse, "flush", None)
        if callable(flush):
            await asyncio.to_thread(flush)
            logger.info("Langfuse client flush completed")
    except Exception:
        logger.exception("Langfuse client flush failed")

    try:
        await asyncio.to_thread(langfuse.shutdown)
        logger.info("Langfuse client shutdown completed")
    except Exception:
        logger.exception("Langfuse client shutdown failed")


def task_error_handler(task: asyncio.Task):
    """
    Background Task에서 발생한 예외를 로그로 출력하는 Handler

    params:
    - task: 예외를 처리할 asyncio.Task 객체

    return: None
    """
    try:
        task.result()
    except asyncio.CancelledError:
        pass 
    except Exception as e:
        logger.error(f"🚨 Background Task '{task.get_name()}' crashed: {e}", exc_info=True)
        os.kill(os.getpid(), signal.SIGTERM)

@asynccontextmanager
async def lifespan(_: FastAPI):
    """
    FastAPI 애플리케이션의 수명 주기 동안 MQ 컨슈머를 실행하는 Lifespan Context Manager

    params:
    - _: FastAPI 애플리케이션 인스턴스

    return: None
    """
    # Langfuse 연결 정보 및 상태를 디버깅
    _debug_langfuse() 

    summarize_task = asyncio.create_task(start_summarize_consumer(), name="mq-summarize-consumer")
    recommend_task = asyncio.create_task(start_recommend_consumer(), name="mq-recommend-consumer")
    
    summarize_task.add_done_callback(task_error_handler)
    recommend_task.add_done_callback(task_error_handler)
    
    logger.info("MQ consumers started")

    try:
        yield
    finally:
        summarize_task.cancel()
        recommend_task.cancel()
        await asyncio.gather(summarize_task, recommend_task, return_exceptions=True)
        await asyncio.shield(_shutdown_langfuse())
        logger.info("MQ consumers stopped")

app = FastAPI(title="BlogTree Worker", lifespan=lifespan)
app.include_router(health_router, prefix="/health", tags=["health"])
