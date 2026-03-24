import asyncio
import logging
import aio_pika
from pydantic import ValidationError

from fastapi_worker.app.core.config import settings
from fastapi_worker.app.mq.recommend_publisher import publish_message
from fastapi_worker.app.services.content_recommender import recommend_keywords
from fastapi_worker.app.schemas.recommend_input import RecommendInputPayload
from fastapi_worker.app.schemas.recommend_response import (
    RecommendResponsePayload, 
    RecommendData, 
    StatusEnum, 
    ErrorData
)

logger = logging.getLogger(__name__)


# Consumer: Input 검증 -> 파이프라인 처리 -> Output 조립 및 검증 -> 전송
async def consume_message(message: aio_pika.IncomingMessage):
    async with message.process(ignore_processed=True):
        # 예외 발생 시 FAILED 처리를 위해 payload 변수를 미리 None으로 선언
        payload = None 
        
        try:
            max_retries = 3

            # [단계 1] Input schema 파싱 및 검증
            payload = RecommendInputPayload.model_validate_json(message.body)
            print(f"[Consumer] 요청 수신 - Task ID: {payload.task_id}")
            
            # LLM 답변 실패 시 최대 3회 재시도
            llm_result = None
            for attempt in range(max_retries):
                llm_result = await asyncio.to_thread(
                    recommend_keywords,
                    career_goal=payload.career_goal, 
                    knowledge_tree=payload.knowledge_tree
                )
                
                # 결과가 성공적으로 나왔다면 루프를 즉시 탈출!
                if llm_result is not None:
                    break 
                    
                # 실패했을 경우 로그를 남기고 다음 루프로 넘어감
                print(f"[Consumer] LLM에서 유효한 결과가 나오지 않았습니다. 재시도 {attempt + 1}/{max_retries}...")
                
                # API 일시 오류일 수 있으므로 1초 정도 쉬었다가 다시 요청
                await asyncio.sleep(1) 

            # 3번을 모두 시도했는데도 결국 None이라면 에러 발생
            if llm_result is None:
                raise ValueError("LLM 키워드 추천 엔진에서 3회 재시도했으나 응답 생성에 실패했습니다.")
            
            # [단계 3] LLM 결과를 RecommendData 스키마로 1차 검증 (형식 강제 변환)
            recommend_data = RecommendData(**llm_result)
            
            # [단계 4] 최종 Output 스키마 조립 및 2차 검증
            response_payload = RecommendResponsePayload(
                task_id=payload.task_id,
                user_id=payload.user_id,
                status=StatusEnum.SUCCESS,
                data=recommend_data
                # error 필드는 Optional이므로 생략 시 자동으로 None 처리됨
            )
            
            # [단계 5] Output Queue로 최종 전송
            await publish_message(settings.RECOMMEND_OUTPUT_QUEUE, response_payload)
            print(f"[Consumer] SUCCESS 메시지 전송 완료: {response_payload.task_id}")
            
            await message.ack() 
            
        except ValidationError as e:
            # Input JSON 자체가 규격 미달인 경우 (치명적 에러)
            print(f"[Consumer] Input 데이터 형식 오류: {e}")
            await message.reject(requeue=False)
            
        except Exception as e:
            print(f"[Consumer] 처리 중 에러 발생: {e}")
            
            # payload가 파싱된 상태에서 에러가 났다면, FAILED 상태로 Output 큐에 응답을 보냅니다.
            if payload:
                error_payload = RecommendResponsePayload(
                    task_id=payload.task_id,
                    user_id=payload.user_id,
                    status=StatusEnum.FAILED,
                    error=ErrorData(
                        code="RECOMMEND_PROCESS_FAILED",
                        message=str(e)
                    )
                )
                await publish_message(settings.RECOMMEND_OUTPUT_QUEUE, error_payload)
                print(f"[Consumer] FAILED 상태 메시지 전송 완료: {payload.task_id}")
                await message.ack() # 서버에 실패 응답을 보냈으므로 MQ에서는 삭제 처리
            else:
                # payload조차 없는 상태의 심각한 에러라면 큐에서 버리거나 재시도
                await message.reject(requeue=False)

async def start_consuming():
    """
    MQ에서 메시지를 지속적으로 수신합니다.
    """
    connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
    channel = await connection.channel()
    
    # prefetch_count를 1로 설정하여 한 번에 하나의 메시지만 처리하도록 최적화
    await channel.set_qos(prefetch_count=1)
    
    queue = await channel.declare_queue(settings.RECOMMEND_INPUT_QUEUE, durable=True)
    
    logger.info("[*] '%s' 큐에서 메시지 대기 중...", settings.RECOMMEND_INPUT_QUEUE)
    await queue.consume(consume_message)
    
    # 커넥션 유지를 위해 무한 대기
    try:
        await asyncio.Future()
    finally:
        await connection.close()