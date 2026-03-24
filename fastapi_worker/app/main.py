import asyncio
import logging
from contextlib import asynccontextmanager

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


@asynccontextmanager
async def lifespan(_: FastAPI):
    summarize_task = asyncio.create_task(start_summarize_consumer(), name="mq-summarize-consumer")
    recommend_task = asyncio.create_task(start_recommend_consumer(), name="mq-recommend-consumer")
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