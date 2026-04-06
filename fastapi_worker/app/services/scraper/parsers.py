import logging
from bs4 import BeautifulSoup
from langfuse import observe

from fastapi_worker.app.services.scraper.utils import (
    clean_and_format_html, 
    is_nested_target, 
    format_tag_text, 
    remove_unwanted_tags,
    TARGET_TAGS
)

logger = logging.getLogger(__name__)

@observe(name="Scraping for Velog", capture_input=False, capture_output=False)
def scrape_for_velog(soup: BeautifulSoup) -> str:
    """
    Velog 맞춤형 텍스트 추출 Parser

    params:
    - soup: BeautifulSoup 객체로 파싱된 HTML 문서

    return: 추출된 본문 텍스트
    - 파싱 실패 또는 빈 텍스트 시 ScrapingParserFailedError 발생
    """
    logger.info("[VELOG] Velog 맞춤형 파싱 파이프라인 시작")
    
    content_tags = soup.find_all(TARGET_TAGS)
    extracted_lines = []
    
    for tag in content_tags:
        if is_nested_target(tag):
            continue

        text = clean_and_format_html(tag)
        if not text or text in ["로그인", "팔로우", "목록 보기"]:
            continue
                
        # "다음 포스트" 등의 키워드가 포함되어 있으면 하단 크롤링 완전 중단
        matched_keyword = next((keyword for keyword in ["관심이 갈 만한 포스트", "이전 포스트", "다음 포스트", "개의 댓글"] if keyword in text), None)
        if matched_keyword:
            logger.info(f"[VELOG] 중단 키워드('{matched_keyword}') 감지로 하단 크롤링 중단")
            break
                    
        # 포맷팅 적용
        formatted_text = format_tag_text(tag.name, text)
        extracted_lines.append(formatted_text)
                
    result_text = '\n\n'.join(extracted_lines).strip()
    logger.info(f"[VELOG] Velog 맞춤형 파싱 파이프라인 완료 (추출된 텍스트 길이: {len(result_text)})")
    
    return result_text


@observe(name="Scraping for Tistory", capture_input=False, capture_output=False)
def scrape_for_tistory(soup: BeautifulSoup) -> str:
    """
    Tistory 맞춤형 텍스트 추출 Parser

    params:
    - soup: BeautifulSoup 객체로 파싱된 HTML 문서

    return: 추출된 본문 텍스트
    - 파싱 실패 또는 빈 텍스트 시 ScrapingParserFailedError 발생
    """
    logger.info("[TISTORY] Tistory 맞춤형 파싱 파이프라인 시작")
    
    target_classes = ['article_view', 'entry-content', 'tt_article_useless_p_margin']
    main_content = None
    
    for class_name in target_classes:
        content = soup.find('div', class_=class_name)
        if content:
            logger.info(f"[TISTORY] 메인 본문 영역 탐색 성공 (class: '{class_name}')")
            main_content = content
            break
            
    if not main_content:
        logger.info("[TISTORY] 지정된 class를 찾지 못해 대체 태그(article/body) 탐색 시도")
        main_content = soup.find('article') or soup.find('body')
        
    # 불필요한 요소 제거 (Tistory 전용 타겟 추가)
    tistory_extras = [
        {'class': 'container_postbtn'},  
        {'class': 'another_category'},   
        {'class': 'comments'},           
        {'id': 'comments'},              
        {'class': 'tags'}                
    ]
    remove_unwanted_tags(main_content, extra_targets=tistory_extras)
                
    content_tags = main_content.find_all(TARGET_TAGS)
    extracted_lines = []
    
    for tag in content_tags:
        if is_nested_target(tag):
            continue

        if tag.name == 'pre':
            text = clean_and_format_html(tag)
        else:
            text = tag.get_text(separator=' ', strip=True)
            text = ' '.join(text.split())
        
        if not text:
            continue

        formatted_text = format_tag_text(tag.name, text)
        extracted_lines.append(formatted_text)
    
    result_text = '\n\n'.join(extracted_lines).strip()
    logger.info(f"[TISTORY] Tistory 맞춤형 파싱 파이프라인 완료 (추출된 텍스트 길이: {len(result_text)})")
    
    return result_text


@observe(name="Scraping for Other Blogs", capture_input=False, capture_output=False)
def scrape_for_else(soup: BeautifulSoup) -> str:
    """
    Velog, Tistory 이외의 일반 블로그용 텍스트 추출 Parser

    params:
    - soup: BeautifulSoup 객체로 파싱된 HTML 문서

    return: 추출된 본문 텍스트
    - 파싱 실패 또는 빈 텍스트 시 ScrapingParserFailedError 발생
    """
    logger.info("[일반 블로그] 공통 파싱 파이프라인 시작")
    
    main_content = soup.find('article') or soup.find('main') or soup.find('body')
    
    # 공통 불필요 태그 제거
    remove_unwanted_tags(main_content)
    
    text_clean = clean_and_format_html(main_content)
    result_text = text_clean.strip()
    
    logger.info(f"[일반 블로그] 공통 파싱 파이프라인 완료 (추출된 텍스트 길이: {len(result_text)})")
    
    return result_text