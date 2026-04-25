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
INITIAL_RETRY_DELAY_SECONDS = 1
MAX_RETRY_DELAY_SECONDS = 5

@observe(name="Recommendation Consume")
async def consume_message(message: aio_pika.IncomingMessage) -> None:
    """
    MQ에서 메시지를 수신하여 Recommendation 파이프라인을 처리하는 Consumer 함수
    - 메시지 수신 시 Input Payload 검증, Recommendation 파이프라인 처리, Output 조립 및 검증, 결과 전송까지의 전체 흐름을 담당하는 핵심 Consumer 함수

    params:
    - message: MQ에서 수신한 메시지 객체
    
    return: None
    - 메시지 처리 성공 시 Output Queue로 결과 전송 후 메시지 ACK
    """
    async with message.process(ignore_processed=True):
        payload = None 
        
        try:
            max_retries = 3
            payload = RecommendInputPayload.model_validate_json(message.body)
            print(f"[Consumer] 요청 수신 - Task ID: {payload.task_id}")

            # Langfuse Trace Context 업데이트
            langfuse = get_client()
            langfuse.update_current_trace(
                session_id=str(payload.task_id),
                user_id=payload.user_id,
                tags=["recommend", f"task:{payload.task_id}"]
            )
            
            llm_result = None
            last_error = None # 마지막으로 발생한 에러를 기억할 변수

            for attempt in range(max_retries):
                try:
                    # LLM API 호출
                    llm_result = await asyncio.to_thread(
                        recommend_keywords,
                        career_goal=payload.career_goal, 
                        knowledge_tree=payload.knowledge_tree
                    )
                    break  # 성공하면 즉시 재시도 루프 탈출
                    
                except Exception as e:
                    # LLMAnswerParserFailedError, LLMAnswerFailedError 등 모든 에러를 여기서 잡음
                    last_error = e
                    print(f"[Consumer] LLM 요청/파싱 에러 발생 ({type(e).__name__}): {e}. 재시도 {attempt + 1}/{max_retries}...")
                    
                    if attempt < max_retries - 1: # LLM 답변 생성 재시도
                        await asyncio.sleep(1)

            # 설정한 횟수만큼 다 돌았는데도 llm_result가 채워지지 않았다면 최종 실패 처리
            if llm_result is None:
                if last_error:
                    print("[Consumer] LLM 키워드 추천 엔진 최대 재시도 횟수 초과. 최종 에러를 던집니다.")
                    raise last_error # 마지막에 3번째에 발생했던 에러를 그대로 위로 던짐
                else:
                    # 예외: last_error가 None인 경우는 거의 없겠지만, 혹시나 LLM 호출 자체가 예외 없이 실패하는 경우를 대비한 안전장치
                    raise LLMAnswerFailedError("LLM 키워드 추천 엔진에서 3회 재시도했으나 응답 생성에 실패했습니다.")
                        
            # 딕셔너리를 객체로 매핑
            recommend_data = RecommendData(**llm_result)
            
            # LLM 응답으로부터 최종 Response Payload 조립 및 검증
            response_payload = RecommendResponsePayload(
                task_id=payload.task_id,
                user_id=payload.user_id,
                status=StatusEnum.SUCCESS,
                data=recommend_data
            )
            
            # Output Queue으로 Publish
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

                # 에러코드 매핑 후 Output Payload 조립 및 검증
                error_payload = RecommendResponsePayload(
                    task_id=payload.task_id,
                    user_id=payload.user_id,
                    status=StatusEnum.FAILED,
                    error=ErrorData(
                        code=error_code,
                        message=str(e)
                    )
                )
                # Output Queue으로 Publish
                await publish_message(settings.RECOMMEND_OUTPUT_QUEUE, error_payload)
                print(f"[Consumer] FAILED ({error_code}) 메시지 전송 완료: {payload.task_id}")
                await message.ack() 
            else:
                await message.reject(requeue=False)

async def start_consuming() -> None:
    """
    MQ에서 메시지를 지속적으로 수신합니다.

    params: None

    return: None
    """
    retry_delay = INITIAL_RETRY_DELAY_SECONDS

    while True:
        connection: aio_pika.RobustConnection | None = None

        try:
            connection = await aio_pika.connect_robust(settings.rabbitmq_url)
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
            retry_delay = INITIAL_RETRY_DELAY_SECONDS
            await queue.consume(consume_message)

            # 커넥션 유지를 위해 무한 대기
            await asyncio.Future()
        except asyncio.CancelledError:
            raise
        except (aio_pika.exceptions.AMQPConnectionError, OSError) as exc:
            logger.warning(
                "RabbitMQ 연결에 실패했습니다. %s초 후 재시도합니다. error=%s",
                retry_delay,
                exc,
            )
            await asyncio.sleep(retry_delay)
            retry_delay = min(retry_delay * 2, MAX_RETRY_DELAY_SECONDS)
        finally:
            if connection and not connection.is_closed:
                await connection.close()
