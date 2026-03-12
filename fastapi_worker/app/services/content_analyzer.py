import json
from typing import Optional, Dict, Any

from fastapi_worker.app.services.llm.summarize import analyze_with_llm
from fastapi_worker.app.services.scraper.scraping import scrape_blog_text


def analyze_blog_content(url: str, knowledge_graph: str) -> Optional[Dict[str, None]]:
    """
    주어진 URL의 블로그 글을 스크래핑하고 LLM을 통해 요약 및 분석합니다.
    
    :param url: 스크래핑할 블로그 URL
    :param knowledge_graph: 페이로드에서 전달받은 지식 그래프 데이터
    :return: 분석된 결과 딕셔너리 (실패 시 None)
    """

    # 1. 스크래핑 
    blog_text = scrape_blog_text(url=url)
    if not blog_text:
        return None

    # 2. LLM 분석 
    llm_result_dict = analyze_with_llm(
        blog_text=blog_text, 
        knowledge_graph=knowledge_graph
    )
    
    if not llm_result_dict:
        return None
    
    # 3. 결과 반환
    return llm_result_dict
