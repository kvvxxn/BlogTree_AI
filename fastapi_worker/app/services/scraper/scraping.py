import requests
import logging
import time
from bs4 import BeautifulSoup
from urllib.parse import urlparse
from langfuse import observe

from fastapi_worker.app.services.scraper.parsers import scrape_forcefully, scrape_for_else, scrape_for_tistory, scrape_for_velog
from fastapi_worker.app.services.scraper.configs import TARGET_TAGS, headers
from fastapi_worker.app.core.exceptions import ScrapingFailedError, ScrapingParserFailedError

logger = logging.getLogger(__name__)

@observe(name="Blog Text Scraping Attempt")
def scrape_blog_text(url: str) -> str:
    """
    Beautiful Soup을 사용하여 블로그 URL에서 본문을 추출하고 블로그 사이트에 맞게 커스텀 파서를 적용
    - 네트워크 요청, Beautiful Soup 파싱, 플랫폼 감지 및 맞춤형 파서 실행, 에러 처리 등을 포함하는 Wrapper 함수

    params:
    - url: 스크래핑할 블로그 URL
    
    return: 추출된 본문 텍스트
    - Beautiful Soup 접근 실패 시: ScrapingFailedError 발생
    - 파싱 실패 또는 빈 텍스트 시: ScrapingParserFailedError 발생
    """
    logger.info(f"[{url}] 웹페이지 스크래핑 요청을 시작합니다.")

    # 네트워크 요청
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        logger.info(f"[{url}] 웹페이지에 성공적으로 접근했습니다. HTTP 상태 코드: {response.status_code}")
        
    except requests.exceptions.RequestException as e:
        logger.error(f"[{url}] 웹페이지 접근 실패 (네트워크/HTTP 에러): {e}")
        raise ScrapingFailedError(f"웹페이지 접근 실패: {str(e)}") # 네트워크 요청 실패시 ScrapingFailedError로 래핑하여 던짐

    # 본문 파싱 및 텍스트 추출 
    try:
        soup = BeautifulSoup(response.text, 'html.parser')
        domain = urlparse(url).netloc.lower()
        text_clean = ""

        is_custom_parser_used = False
        
        # Velog 맞춤형 스크래핑 
        if 'velog.io' in domain:
            logger.info(f"[VELOG] 플랫폼 감지 - 맞춤형 파서 실행")
            started_at = time.perf_counter()
            text_clean = scrape_for_velog(soup)
            logger.info("[VELOG] 파서 실행 시간: %.3fs", time.perf_counter() - started_at)
            is_custom_parser_used = True

        # Tistory 맞춤형 스크래핑 
        elif 'tistory.com' in domain:
            logger.info(f"[TISTORY] 플랫폼 감지 - 맞춤형 파서 실행")
            started_at = time.perf_counter()
            text_clean = scrape_for_tistory(soup)
            logger.info("[TISTORY] 파서 실행 시간: %.3fs", time.perf_counter() - started_at)
            is_custom_parser_used = True

        # 그 외 일반 블로그 공통 스크래핑 
        else:
            logger.info("[일반 블로그] 플랫폼 감지 - Trafilatura 강제 파서 즉시 실행")
            started_at = time.perf_counter()
            text_clean = scrape_forcefully(html_source=response.text, url=url) # 무조건 Trafilatura로 스크래핑
            logger.info("[일반 블로그] 파서 실행 시간: %.3fs", time.perf_counter() - started_at)

        # 파싱 실패 또는 빈 텍스트인지 확인
        if is_custom_parser_used and (not text_clean or not text_clean.strip()):
            logger.warning(f"[{url}] 파서는 실행되었으나 텍스트 추출 결과가 비어있습니다.")

            logger.info(f"[{url}] trafilatura을 이용하여 강제 스크래핑을 시도합니다.")
            started_at = time.perf_counter()
            text_clean = scrape_forcefully(html_source=response.text, url=url)
            logger.info("강제 스크래핑 실행 시간: %.3fs", time.perf_counter() - started_at)

        if not text_clean or not text_clean.strip():
            raise ScrapingParserFailedError("유의미한 본문 텍스트를 추출하지 못했습니다.")

        logger.info(f"[{url}] 스크래핑 완료! 총 {len(text_clean)}자 추출됨.")
        return text_clean

    except ScrapingParserFailedError:
        # 위에서 의도적으로 발생시킨 빈 텍스트 에러는 그대로 던짐
        raise
    except Exception as e:
        # BeautifulSoup 파싱 오류, scrape_for_* 함수 내부 로직 오류 등
        logger.error(f"[{url}] 텍스트 파싱 중 알 수 없는 오류 발생: {e}")
        raise ScrapingParserFailedError(f"본문 파싱 중 오류 발생: {str(e)}")