from pydantic import BaseModel, HttpUrl, Field, UUID4
from typing import Dict, List
from datetime import datetime

class RecommendInputPayload(BaseModel):
    """
    MQ에서 전달받는 추천 작업 Input 스키마
    """
    task_id: UUID4 = Field(..., description="작업 고유 ID")
    user_id: UUID4 = Field(..., description="사용자 고유 ID")
    
    career_goal: str = Field(..., description="사용자의 커리어 목표 (예: AI Engineer)")
    
    expired_at: datetime = Field(..., description="작업 만료 시간")
    
    # 구조: 'Category(str) -> Topic(str) -> Keyword(List[str])' 형태
    knowledge_tree: Dict[str, Dict[str, List[str]]] = Field(
        default_factory=dict, 
        description="추천 시 참고할 사용자의 지식 트리 (배경 지식 컨텍스트)"
    )