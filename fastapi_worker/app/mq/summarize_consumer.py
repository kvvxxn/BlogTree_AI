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


def _to_knowledge_tree(raw_keywords) -> KnowledgeTree | None:
    if not isinstance(raw_keywords, list) or len(raw_keywords) != 3:
        return None
    if not all(isinstance(item, str) and item.strip() for item in raw_keywords):
        return None
    return KnowledgeTree(category=raw_keywords[0], topic=raw_keywords[1], keyword=raw_keywords[2])

# Consumer: Input 검증 -> 파이프라인 처리 -> Output 조립 및 검증 -> 전송
@observe(name="Summarization Consume")
async def consume_message(message: aio_pika.IncomingMessage):
    async with message.process(ignore_processed=True):
        payload = None 
        
        try:
            max_retries = 3

            # [단계 1] Input schema 파싱 및 검증
            payload = SummarizeInputPayload.model_validate_json(message.body)
            print(f"[Consumer] 요청 수신 - Task ID: {payload.task_id}")

            # Langfuse Trace Context 업데이트
            langfuse = get_client()
            langfuse.update_current_trace(
                # v4부터는 id= 를 맘대로 지정할 수 없으므로, session_id나 태그로 관리합니다.
                session_id=payload.task_id, 
                user_id=payload.user_id,
                tags=["summarize", f"task:{payload.task_id}"] # 검색 편의를 위해 태그 추가
            )
            
            # [단계 2] 파이프라인 호출 
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
                    # 블로그가 막혀있거나 구조가 바뀐 경우는 재시도해도 무의미하므로 즉시 에러 던짐
                    print(f"[Consumer] 스크래핑 에러 감지 (재시도 안 함): {e}")
                    raise e
                    
                except Exception as api_err:
                    # LLM API 타임아웃 등의 오류는 재시도
                    print(f"[Consumer] 파이프라인 오류 발생: {api_err}. 재시도 {attempt + 1}/{max_retries}...")
                    await asyncio.sleep(1) 

            # 3번 시도 후에도 결과가 없다면
            if llm_result is None:
                raise LLMAnswerFailedError("LLM 요약 엔진에서 3회 재시도했으나 응답 생성에 실패했습니다.")
            
            # [단계 3] 지식 트리 파싱 및 검증
            parsed_knowledge_tree = _to_knowledge_tree(llm_result.get("keywords"))
            
            # 지식 트리 추출에 실패했다면 에러 처리 (기존 PARTIAL_SUCCESS 대체)
            if parsed_knowledge_tree is None:
                raise LLMAnswerParserFailedError("LLM 답변에서 올바른 형태의 지식 트리를 추출하지 못했습니다.")

            response_data = ResponseData(
                summary_content=llm_result["summary"],
                knowledge_tree=parsed_knowledge_tree
            )

            # [단계 4] 최종 Output 스키마 조립 (항상 SUCCESS)
            response_payload = SummarizeResponsePayload(
                task_id=payload.task_id,
                user_id=payload.user_id,
                status=StatusEnum.SUCCESS,
                data=response_data
            )
            
            # [단계 5] Output Queue로 최종 전송
            await publish_message(settings.SUMMARIZE_OUTPUT_QUEUE, response_payload)
            print(f"[Consumer] SUCCESS 메시지 전송 완료: {response_payload.task_id}")
            
            await message.ack() 
            
        except ValidationError as e:
            print(f"[Consumer] Input 데이터 형식 오류: {e}")
            # 페이로드 자체를 못 읽었을 때의 처리 (원시 JSON에서 task_id 추출 로직 등을 추가할 수 있음)
            await message.reject(requeue=False)
            
        except Exception as e:
            print(f"[Consumer] 처리 중 FAILED 발생: {e}")
            
            if payload:
                # [핵심] 발생한 예외 타입에 따라 우리가 정의한 에러 코드로 매핑
                error_code = "UNKNOWN_ERROR"
                if isinstance(e, ScrapingFailedError):
                    error_code = "SCRAPING_FAILED"
                elif isinstance(e, ScrapingParserFailedError):
                    error_code = "SCRAPING_PARSER_FAILED"
                elif isinstance(e, LLMAnswerFailedError):
                    error_code = "LLM_ANSWER_FAILED"
                elif isinstance(e, LLMAnswerParserFailedError):
                    error_code = "LLM_ANSWER_PARSER_FAILED"

                error_payload = SummarizeResponsePayload(
                    task_id=payload.task_id,
                    user_id=payload.user_id,
                    status=StatusEnum.FAILED,
                    error=ErrorData(
                        code=error_code,
                        message=str(e)
                    )
                )
                await publish_message(settings.SUMMARIZE_OUTPUT_QUEUE, error_payload)
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
        settings.SUMMARIZE_INPUT_QUEUE, 
        durable=True,
        arguments={
            "x-dead-letter-exchange": "dlx.exchange",
            "x-dead-letter-routing-key": "summary.request.dead"  
        }
    )
    
    logger.info("[*] '%s' 큐에서 메시지 대기 중...", settings.SUMMARIZE_INPUT_QUEUE)
    await queue.consume(consume_message)
    
    # 커넥션 유지를 위해 무한 대기
    try:
        await asyncio.Future()
    finally:
        await connection.close()