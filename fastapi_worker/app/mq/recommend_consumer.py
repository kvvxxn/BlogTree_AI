import asyncio
import logging
import aio_pika
from pydantic import ValidationError
from langfuse import observe, get_client

from fastapi_worker.app.core.config import settings
from fastapi_worker.app.mq.recommend_publisher import publish_message
from fastapi_worker.app.services.content_recommender import recommend_keywords
from fastapi_worker.app.schemas.recommend_input import RecommendInputPayload
from fastapi_worker.app.core.exceptions import LLMAnswerFailedError, LLMAnswerParserFailedError
from fastapi_worker.app.schemas.recommend_response import (
    RecommendResponsePayload, 
    RecommendData, 
    StatusEnum, 
    ErrorData
)

logger = logging.getLogger(__name__)

# Consumer: Input 검증 -> 파이프라인 처리 -> Output 조립 및 검증 -> 전송
@observe(name="Recommendation Consume")
async def consume_message(message: aio_pika.IncomingMessage):
    async with message.process(ignore_processed=True):
        payload = None 
        
        try:
            max_retries = 3
            payload = RecommendInputPayload.model_validate_json(message.body)
            print(f"[Consumer] 요청 수신 - Task ID: {payload.task_id}")

            # Langfuse Trace Context 업데이트
            langfuse = get_client()
            langfuse.update_current_trace(
                session_id=payload.task_id,
                user_id=payload.user_id,
                tags=["recommend", f"task:{payload.task_id}"]
            )
            
            llm_result = None
            for attempt in range(max_retries):
                try:
                    llm_result = await asyncio.to_thread(
                        recommend_keywords,
                        career_goal=payload.career_goal, 
                        knowledge_tree=payload.knowledge_tree
                    )
                    break 
                except LLMAnswerParserFailedError as e:
                    # 응답 형식이 깨진 경우는 재시도하지 않고 바로 에러 처리
                    print(f"[Consumer] LLM 파싱 에러 감지 (재시도 안 함): {e}")
                    raise e
                except Exception as api_err:
                    print(f"[Consumer] LLM 요청 중 에러 발생: {api_err}. 재시도 {attempt + 1}/{max_retries}...")
                    await asyncio.sleep(1)

            if llm_result is None:
                raise LLMAnswerFailedError("LLM 키워드 추천 엔진에서 3회 재시도했으나 응답 생성에 실패했습니다.")
            
            # 딕셔너리를 객체로 매핑 (위에서 에러가 안 났다면 무조건 성공)
            recommend_data = RecommendData(**llm_result)
            
            response_payload = RecommendResponsePayload(
                task_id=payload.task_id,
                user_id=payload.user_id,
                status=StatusEnum.SUCCESS,
                data=recommend_data
            )
            
            await publish_message(settings.RECOMMEND_OUTPUT_QUEUE, response_payload)
            print(f"[Consumer] SUCCESS 메시지 전송 완료: {response_payload.task_id}")
            await message.ack() 
            
        except ValidationError as e:
            print(f"[Consumer] Input 데이터 형식 오류: {e}")
            await message.reject(requeue=False)
            
        except Exception as e:
            print(f"[Consumer] 처리 중 FAILED 발생: {e}")
            
            if payload:
                # 에러 코드 매핑 로직
                error_code = "UNKNOWN_ERROR"
                if isinstance(e, LLMAnswerFailedError):
                    error_code = "LLM_ANSWER_FAILED"
                elif isinstance(e, LLMAnswerParserFailedError):
                    error_code = "LLM_ANSWER_PARSER_FAILED"

                error_payload = RecommendResponsePayload(
                    task_id=payload.task_id,
                    user_id=payload.user_id,
                    status=StatusEnum.FAILED,
                    error=ErrorData(
                        code=error_code,
                        message=str(e)
                    )
                )
                await publish_message(settings.RECOMMEND_OUTPUT_QUEUE, error_payload)
                print(f"[Consumer] FAILED ({error_code}) 메시지 전송 완료: {payload.task_id}")
                await message.ack() 
            else:
                await message.reject(requeue=False)

async def start_consuming():
    """
    MQ에서 메시지를 지속적으로 수신합니다.
    """
    connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
    channel = await connection.channel()
    
    # prefetch_count를 1로 설정하여 한 번에 하나의 메시지만 처리하도록 최적화
    await channel.set_qos(prefetch_count=1)
    
    queue = await channel.declare_queue(
        settings.RECOMMEND_INPUT_QUEUE, 
        durable=True,
        arguments={
            "x-dead-letter-exchange": "dlx.exchange",
            "x-dead-letter-routing-key": "recommend.request.dead" 
        }
    )
    
    logger.info("[*] '%s' 큐에서 메시지 대기 중...", settings.RECOMMEND_INPUT_QUEUE)
    await queue.consume(consume_message)
    
    # 커넥션 유지를 위해 무한 대기
    try:
        await asyncio.Future()
    finally:
        await connection.close()