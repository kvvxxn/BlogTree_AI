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
    category: str = Field(..., description="대분류 (예: Backend)")
    topic: str = Field(..., description="중분류 (예: Database)")
    keyword: str = Field(..., description="추천 대상 핵심 키워드 (예: Transaction Isolation)")

class RecommendData(BaseModel):
    """
    실제 추천 결과 데이터 스키마 (SUCCESS 상태일 때 완벽한 형태로 존재)
    """
    recommend_reason: str = Field(..., description="추천 이유")
    knowledge_tree: KnowledgeTree = Field(..., description="추천 대상 지식 트리")

class ErrorData(BaseModel):
    code: str = Field(..., description="에러 코드")
    message: str = Field(..., description="실패 원인 상세 메시지")

class RecommendResponsePayload(BaseModel):
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: int = Field(..., description="사용자 고유 ID")
    status: StatusEnum = Field(..., description="처리 결과 상태")
    
    data: Optional[RecommendData] = Field(None, description="추천 결과 데이터")
    error: Optional[ErrorData] = Field(None, description="에러 정보")