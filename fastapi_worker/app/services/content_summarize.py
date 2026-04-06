import json
import logging
from typing import Optional, Dict, Any
from langfuse import observe

from fastapi_worker.app.services.llm.summarize import summarize_with_llm
from fastapi_worker.app.services.scraper.scraping import scrape_blog_text

logger = logging.getLogger(__name__)

@observe(name="Summarization Wrapper function called")
def summarize_blog_content(career_goal: str, url: str, knowledge_tree: str) -> Optional[Dict[str, None]]:
    """
    주어진 URL의 블로그 글을 스크래핑하고 LLM을 통해 요약 및 분석
    - Summarization Wrapper 함수로 블로그 스크래핑과 LLM 분석을 하나의 파이프라인으로 묶어 처리
    
    params:
    - career_goal: 사용자의 경력 목표 
    - url: 스크래핑할 블로그 URL
    - knowledge_tree: 페이로드에서 전달받은 전체 지식 그래프 데이터
    
    return: 분석된 결과 딕셔너리
    - LLM 응답이 비어있거나 JSON 파싱 실패 시 LLMAnswerParserFailedError 발생
    - LLM API 호출 실패 시 LLMAnswerFailedError 발생

    """
    logger.info(f"[Pipeline Start] URL: {url} 분석 파이프라인 시작")

    # 1. 스크래핑 
    blog_text = scrape_blog_text(url=url)

    # 2. LLM 분석 
    llm_result_dict = summarize_with_llm(
        career_goal=career_goal,
        url=url,
        blog_text=blog_text, 
        knowledge_tree=knowledge_tree
    )
    
    # 3. 결과 반환
    logger.info(f"[Pipeline Success] URL: {url} 분석 파이프라인 완료")
    return llm_result_dict
