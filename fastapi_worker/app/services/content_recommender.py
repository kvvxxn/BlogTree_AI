import json
import logging
from typing import Optional, Dict, Any
from langfuse import observe

from fastapi_worker.app.services.llm.recommend import recommend_with_llm

logger = logging.getLogger(__name__)

@observe(name="Recommendation Wrapper function called")
def recommend_keywords(career_goal: str, knowledge_tree: str) -> Dict[str, Any]:
    """
    사용자의 커리어 목표와 기존 지식 트리를 바탕으로 LLM을 통해 맞춤형 키워드를 추천
    - Recommendation Wrapper 함수로 LLM 추천을 하나의 파이프라인으로 묶어 처리
    
    params:
    - career_goal: 사용자의 커리어 목표 
    - param knowledge_tree: 페이로드에서 전달받은 전체 지식 트리 데이터
    
    return: 추천 결과 딕셔너리 
    - LLM 응답이 비어있거나 JSON 파싱 실패 시 LLMAnswerParserFailedError 발생
    - LLM API 호출 실패 시 LLMAnswerFailedError 발생
    """
    logger.info("[Pipeline Start] 커리어 목표 기반 키워드 추천 파이프라인 시작")

    # 예외가 발생하면 바로 상위(컨슈머)로 던져짐
    llm_result_dict = recommend_with_llm(
        career_goal=career_goal,
        knowledge_tree=knowledge_tree
    )
    
    logger.info("[Pipeline Success] 키워드 추천 파이프라인 완료")
    return llm_result_dict