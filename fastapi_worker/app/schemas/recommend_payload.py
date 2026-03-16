from pydantic import BaseModel, HttpUrl, Field, UUID4
from typing import Dict, List

class SummarizeInputPayload(BaseModel):
    """
    MQ에서 전달받는 요약 작업 Input 스키마
    """
    # UUID4 타입으로 지정하면 형식이 올바른지 Pydantic이 자동으로 검증해 줍니다.
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: UUID4 = Field(..., description="사용자 고유 ID")
    
    career_goal: str = Field(..., description="사용자의 커리어 목표 (예: AI Engineer)")
    target_url: HttpUrl = Field(alias="source_url", description="크롤링 및 요약할 대상 URL")
    
    # context: '분야명(str) -> 하위 카테고리(str) -> 키워드 목록(List[str])' 형태
    context: Dict[str, Dict[str, List[str]]] = Field(
        default_factory=dict, 
        description="요약 시 참고할 사용자의 배경 지식 컨텍스트"
    )