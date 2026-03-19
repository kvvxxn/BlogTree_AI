import json
import logging
from typing import Optional, Dict, Any

from fastapi_worker.app.services.llm.recommend import recommend_with_llm

logger = logging.getLogger(__name__)

def recommend_keywords(career_goal: str, knowledge_tree: str) -> Optional[Dict[str, Any]]:
    """
    사용자의 커리어 목표와 기존 지식 트리를 바탕으로 LLM을 통해 맞춤형 키워드를 추천합니다.
    
    :param career_goal: 사용자의 커리어 목표 (예: AI Engineer)
    :param knowledge_tree: 페이로드에서 전달받은 지식 트리 데이터
    :return: 추천 결과 딕셔너리 (실패 시 None)
    """
    logger.info("[Pipeline Start] 커리어 목표 기반 키워드 추천 파이프라인 시작")

    # 1. LLM 분석 (키워드 추천)
    llm_result_dict = recommend_with_llm(
        career_goal=career_goal,
        knowledge_tree=knowledge_tree
    )
    
    if not llm_result_dict:
        logger.warning("[Pipeline Stop] LLM 키워드 추천 분석 실패로 파이프라인 중단")
        return None
    
    # 2. 결과 반환
    logger.info("[Pipeline Success] 키워드 추천 파이프라인 완료")
    return llm_result_dict