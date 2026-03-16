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
    keyword: str = Field(..., description="핵심 키워드 (예: Transaction Isolation)")

class ResponseData(BaseModel):
    """
    실제 결과 데이터 스키마
    Optional을 사용하여 값이 없을 때 JSON에서 null로 처리되도록 합니다.
    """
    summary_content: Optional[str] = Field(None, description="요약된 본문 내용 (실패 시 null)")
    knowledge_tree: Optional[KnowledgeTree] = Field(None, description="추출된 지식 트리 (부분 실패 또는 실패 시 null)")
    error_message: Optional[str] = Field(None, description="실패 원인 메시지 (FAILED 상태일 때 주로 사용)")

class SummarizeResponsePayload(BaseModel):
    """
    MQ로 다시 보낼 최종 Output 스키마
    """
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: UUID4 = Field(..., description="사용자 고유 ID")
    status: StatusEnum = Field(..., description="처리 결과 상태")
    
    # FAILED 상태일 때는 data 자체가 null일 수도 있으므로 Optional 처리
    data: Optional[ResponseData] = Field(None, description="결과 데이터")