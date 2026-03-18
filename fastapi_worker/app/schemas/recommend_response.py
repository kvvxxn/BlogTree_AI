from enum import Enum
from pydantic import BaseModel, Field, UUID4
from typing import Optional

class StatusEnum(str, Enum):
    """
    작업 상태를 엄격하게 관리하기 위한 Enum 클래스
    """
    SUCCESS = "SUCCESS"
    PARTIAL_SUCCESS = "PARTIAL_SUCCESS"
    FAILED = "FAILED"

class KnowledgeTree(BaseModel):
    """
    추출된 키워드 트리 스키마
    """
    category: str = Field(..., description="대분류 (예: Backend)")
    topic: str = Field(..., description="중분류 (예: Database)")
    keyword: str = Field(..., description="추천 대상 핵심 키워드 (예: Transaction Isolation)")

class RecommendData(BaseModel):
    """
    실제 추천 결과 데이터 스키마
    PARTIAL_SUCCESS 상태를 고려하여 각 필드를 Optional로 처리합니다.
    """
    recommend_reason: Optional[str] = Field(None, description="추천 이유 (실패 시 null)")
    knowledge_tree: Optional[KnowledgeTree] = Field(None, description="추천 대상 지식 트리 (실패 시 null)")

class ErrorData(BaseModel):
    """
    Failed일 때 사용하는 구체적인 필드
    """
    code: str = Field(..., description="에러 코드 (예: RECOMMEND_FAILED)")
    message: str = Field(..., description="실패 원인 상세 메시지")

class RecommendResponsePayload(BaseModel):
    """
    MQ로 다시 보낼 추천 작업의 최종 Output 스키마
    """
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: UUID4 = Field(..., description="사용자 고유 ID")
    status: StatusEnum = Field(..., description="처리 결과 상태")
    
    # SUCCESS 또는 PARTIAL_SUCCESS 일 때 존재, FAILED 일 때 null
    data: Optional[RecommendData] = Field(None, description="추천 결과 데이터")
    
    # FAILED 일 때 존재, SUCCESS 또는 PARTIAL_SUCCESS 일 때 null
    error: Optional[ErrorData] = Field(None, description="에러 정보")