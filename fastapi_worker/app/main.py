import os
import asyncio
import logging
from contextlib import asynccontextmanager
from dotenv import load_dotenv

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
        logger.info("MQ consumers stopped")

app = FastAPI(title="BlogTree Worker", lifespan=lifespan)
app.include_router(health_router, prefix="/health", tags=["health"])