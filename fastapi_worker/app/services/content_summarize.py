import json
import logging
from typing import Optional, Dict, Any
from langfuse.decorators import observe

from fastapi_worker.app.services.llm.summarize import summarize_with_llm
from fastapi_worker.app.services.scraper.scraping import scrape_blog_text

logger = logging.getLogger(__name__)

@observe("Summarization Wrapper function called")
def summarize_blog_content(career_goal: str, url: str, knowledge_tree: str) -> Optional[Dict[str, None]]:
    """
    주어진 URL의 블로그 글을 스크래핑하고 LLM을 통해 요약 및 분석합니다.
    
    :param career_goal: 사용자의 경력 목표 (예: AI Engineer)
    :param url: 스크래핑할 블로그 URL
    :param knowledge_tree: 페이로드에서 전달받은 지식 그래프 데이터
    :return: 분석된 결과 딕셔너리 (실패 시 None)
    """
    logger.info(f"[Pipeline Start] URL: {url} 분석 파이프라인 시작")

    # 1. 스크래핑 
    blog_text = scrape_blog_text(url=url)
    if not blog_text:
        logger.warning(f"[Pipeline Stop] URL: {url} 스크래핑 실패로 파이프라인 중단")
        return None

    # 2. LLM 분석 
    llm_result_dict = summarize_with_llm(
        career_goal=career_goal,
        url=url,
        blog_text=blog_text, 
        knowledge_tree=knowledge_tree
    )
    
    if not llm_result_dict:
        logger.warning(f"[Pipeline Stop] URL: {url} LLM 분석 실패로 파이프라인 중단")
        return None
    
    # 3. 결과 반환
    logger.info(f"[Pipeline Success] URL: {url} 분석 파이프라인 완료")
    return llm_result_dict
