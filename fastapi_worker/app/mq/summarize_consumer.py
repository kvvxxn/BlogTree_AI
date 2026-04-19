import asyncio
import logging
import aio_pika
from pydantic import ValidationError
from langfuse import observe, get_client

from fastapi_worker.app.core.config import settings
from fastapi_worker.app.mq.summarize_publisher import publish_message 
from fastapi_worker.app.services.content_summarize import summarize_blog_content

from fastapi_worker.app.schemas.summarize_input import SummarizeInputPayload
from fastapi_worker.app.schemas.summarize_response import (
    SummarizeResponsePayload, 
    ResponseData, 
    KnowledgeTree,
    StatusEnum, 
    ErrorData
)
from fastapi_worker.app.core.exceptions import (
    ScrapingFailedError, 
    ScrapingParserFailedError, 
    LLMAnswerFailedError, 
    LLMAnswerParserFailedError
)

logger = logging.getLogger(__name__)
INITIAL_RETRY_DELAY_SECONDS = 1
MAX_RETRY_DELAY_SECONDS = 5


def _to_knowledge_tree(raw_keywords) -> KnowledgeTree | None:
    """
    LLM이 반환한 키워드 리스트를 KnowledgeTree 객체로 변환하는 함수

    params:
    - raw_keywords: LLM 응답에서 추출한 키워드 리스트 

    return: KnowledgeTree 객체
    - raw_keywords가 리스트가 아니거나 길이가 3이 아닌 경우 None 반환
    """
    if not isinstance(raw_keywords, list) or len(raw_keywords) != 3:
        return None
    if not all(isinstance(item, str) and item.strip() for item in raw_keywords):
        return None
    return KnowledgeTree(category=raw_keywords[0], topic=raw_keywords[1], keyword=raw_keywords[2])

@observe(name="Summarization Consume")
async def consume_message(message: aio_pika.IncomingMessage) -> None:
    """
    MQ에서 메시지를 수신하여 Summarization 파이프라인을 처리하는 Consumer 함수
    - 메시지 수신 시 Input Payload 검증, Summarization 파이프라인 처리, Output 조립 및 검증, 결과 전송까지의 전체 흐름을 담당하는 핵심 Consumer 함수
    
    params:
    - message: MQ에서 수신한 메시지 객체 

    return: None
    - 메시지 처리 성공 시 Output Queue로 결과 전송 후 메시지 ACK
    """
    async with message.process(ignore_processed=True):
        payload = None 
        
        try:
            max_retries = 3

            # Input schema 파싱 및 검증
            payload = SummarizeInputPayload.model_validate_json(message.body)
            print(f"[Consumer] 요청 수신 - Task ID: {payload.task_id}")

            # Langfuse Trace Context 업데이트
            langfuse = get_client()
            langfuse.update_current_trace(
                session_id=str(payload.task_id),
                user_id=payload.user_id,
                tags=["summarize", f"task:{payload.task_id}"] # 검색 편의를 위해 태그 추가
            )
            
            # Summarize 파이프라인 호출 
            llm_result = None
            
            for attempt in range(max_retries):
                try:
                    llm_result = await asyncio.to_thread(
                        summarize_blog_content,
                        career_goal=payload.career_goal, 
                        url=str(payload.source_url), 
                        knowledge_tree=payload.knowledge_tree
                    )
                    break # 성공 시 루프 탈출
                    
                except (ScrapingFailedError, ScrapingParserFailedError) as e:
                    # 스크래핑 관련 에러는 재시도 하지 않음
                    # 스크래핑 실패 시, 재시도 의미 없을 가능성이 높음
                    print(f"[Consumer] 스크래핑 에러 감지 (재시도 안 함): {e}")
                    raise e
                    
                except Exception as api_err:
                    # LLM API 타임아웃 등의 오류는 재시도
                    print(f"[Consumer] 파이프라인 오류 발생: {api_err}. 재시도 {attempt + 1}/{max_retries}...")
                    await asyncio.sleep(1) 

            # LLM API 호출을 3회 재시도했음에도 결과가 없으면 에러 처리
            if llm_result is None:
                raise LLMAnswerFailedError("LLM 요약 엔진에서 3회 재시도했으나 응답 생성에 실패했습니다.")
            
            # LLM 답변으로부터 Knowledge Tree 파싱 및 검증
            parsed_knowledge_tree = _to_knowledge_tree(llm_result.get("keywords"))
            
            # Knowledge Tree 추출에 실패했다면 에러 처리
            if parsed_knowledge_tree is None:
                raise LLMAnswerParserFailedError("LLM 답변에서 올바른 형태의 지식 트리를 추출하지 못했습니다.")

            response_data = ResponseData(
                summary_content=llm_result["summary"],
                knowledge_tree=parsed_knowledge_tree
            )

            # LLM 응답으로부터 최종 Output Payload 조립 및 검증
            response_payload = SummarizeResponsePayload(
                task_id=payload.task_id,
                user_id=payload.user_id,
                status=StatusEnum.SUCCESS,
                data=response_data
            )
            
            # Output Queue으로 Publish
            await publish_message(settings.SUMMARIZE_OUTPUT_QUEUE, response_payload)
            print(f"[Consumer] SUCCESS 메시지 전송 완료: {response_payload.task_id}")
            
            await message.ack() 
            
        except ValidationError as e:
            print(f"[Consumer] Input 데이터 형식 오류: {e}")
            # Input payload 자체 형식 오류는 메시지 거부
            await message.reject(requeue=False)
            
        except Exception as e:
            print(f"[Consumer] 처리 중 FAILED 발생: {e}")
            
            if payload:
                # 발생한 예외 타입에 따라 우리가 정의한 에러 코드로 매핑
                error_code = "UNKNOWN_ERROR"
                if isinstance(e, ScrapingFailedError):
                    error_code = "SCRAPING_FAILED"
                elif isinstance(e, ScrapingParserFailedError):
                    error_code = "SCRAPING_PARSER_FAILED"
                elif isinstance(e, LLMAnswerFailedError):
                    error_code = "LLM_ANSWER_FAILED"
                elif isinstance(e, LLMAnswerParserFailedError):
                    error_code = "LLM_ANSWER_PARSER_FAILED"

                # 에러코드 매핑 후 Output Payload 조립 및 검증
                error_payload = SummarizeResponsePayload(
                    task_id=payload.task_id,
                    user_id=payload.user_id,
                    status=StatusEnum.FAILED,
                    error=ErrorData(
                        code=error_code,
                        message=str(e)
                    )
                )
                # Output Queue으로 Publish
                await publish_message(settings.SUMMARIZE_OUTPUT_QUEUE, error_payload)
                print(f"[Consumer] FAILED ({error_code}) 메시지 전송 완료: {payload.task_id}")
                await message.ack() 
            else:
                await message.reject(requeue=False)

async def start_consuming() -> None:
    """
    Message Queue에서 메시지를 지속적으로 수신합니다.

    params: None

    return: None
    """
    retry_delay = INITIAL_RETRY_DELAY_SECONDS

    while True:
        connection: aio_pika.RobustConnection | None = None

        try:
            connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
            channel = await connection.channel()

            # prefetch_count를 1로 설정하여 한 번에 하나의 메시지만 처리하도록 최적화
            await channel.set_qos(prefetch_count=1)

            queue = await channel.declare_queue(
                settings.SUMMARIZE_INPUT_QUEUE,
                durable=True,
                arguments={
                    "x-dead-letter-exchange": "dlx.exchange",
                    "x-dead-letter-routing-key": "summary.request.dead"
                }
            )

            logger.info("[*] '%s' 큐에서 메시지 대기 중...", settings.SUMMARIZE_INPUT_QUEUE)
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
