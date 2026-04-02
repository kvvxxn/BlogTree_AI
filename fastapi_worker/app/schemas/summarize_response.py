from enum import Enum
from pydantic import BaseModel, Field, UUID4
from typing import Optional

class StatusEnum(str, Enum):
    """
    작업 상태를 엄격하게 관리하기 위한 Enum 클래스
    """
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"

class KnowledgeTree(BaseModel):
    """
    추출된 키워드 트리 스키마
    """
    category: str = Field(..., description="대분류 (예: Backend)")
    topic: str = Field(..., description="중분류 (예: Database)")
    keyword: str = Field(..., description="핵심 키워드 (예: Transaction Isolation)")

class ResponseData(BaseModel):
    """
    실제 결과 데이터 스키마 (SUCCESS 상태일 때 사용)
    """
    summary_content: str = Field(..., description="요약된 본문 내용")
    knowledge_tree: Optional[KnowledgeTree] = Field(None, description="추출된 지식 트리 (키워드)")

class ErrorData(BaseModel):
    """
    Failed일 때 사용하는 구체적인 필드
    """
    code: str = Field(..., description="에러 코드 (예: SCRAPING_BLOCKED, SUMMARY_FAILED)")
    message: str = Field(..., description="실패 원인 상세 메시지")

class SummarizeResponsePayload(BaseModel):
    """
    MQ로 다시 보낼 최종 Output 스키마
    """
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: int = Field(..., description="사용자 고유 ID")
    status: StatusEnum = Field(..., description="처리 결과 상태")
    
    # SUCCESS 일 때 존재, FAILED 일 때 null
    data: Optional[ResponseData] = Field(None, description="결과 데이터")
    
    # FAILED 일 때 존재, SUCCESS 일 때 null
    error: Optional[ErrorData] = Field(None, description="에러 정보")