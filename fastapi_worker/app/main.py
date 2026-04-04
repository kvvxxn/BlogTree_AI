import os
import asyncio
import logging
from contextlib import asynccontextmanager
from dotenv import load_dotenv
from langfuse import get_client

from fastapi import FastAPI

from fastapi_worker.app.api.health import router as health_router
from fastapi_worker.app.mq.recommend_consumer import start_consuming as start_recommend_consumer
from fastapi_worker.app.mq.summarize_consumer import start_consuming as start_summarize_consumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(filename)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

logger = logging.getLogger(__name__)

load_dotenv()  # .env 파일에서 환경 변수 로드

def _mask_secret(value: str | None) -> str:
    if not value:
        return "MISSING"
    if len(value) <= 10:
        return value[:2] + "***"
    return value[:6] + "..." + value[-4:]

def _debug_langfuse() -> None:
    public_key = os.getenv("LANGFUSE_PUBLIC_KEY")
    secret_key = os.getenv("LANGFUSE_SECRET_KEY")
    host = os.getenv("LANGFUSE_HOST")
    base_url = os.getenv("LANGFUSE_BASE_URL")

    logger.info(
        "[Langfuse Debug] PUBLIC=%s SECRET=%s HOST=%s BASE_URL=%s",
        _mask_secret(public_key),
        _mask_secret(secret_key),
        host or "MISSING",
        base_url or "MISSING",
    )

    try:
        langfuse = get_client()
        logger.info("[Langfuse Debug] auth_check=%s", langfuse.auth_check())
    except Exception:
        logger.exception("[Langfuse Debug] auth_check failed")


async def _shutdown_langfuse() -> None:
    """Flush pending telemetry and then shutdown Langfuse client gracefully."""
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

# Background Task의에러를 출력하는 함수
def task_error_handler(task: asyncio.Task):
    try:
        task.result()
    except asyncio.CancelledError:
        pass  # 정상 종료 시 무시
    except Exception as e:
        logger.error(f"🚨 Background Task '{task.get_name()}' crashed: {e}", exc_info=True)

@asynccontextmanager
async def lifespan(_: FastAPI):
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