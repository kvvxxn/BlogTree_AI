import requests
import logging
from bs4 import BeautifulSoup
from urllib.parse import urlparse
from typing import Optional, Dict, Any

from fastapi_worker.app.services.scraper.parsers import scrape_for_else, scrape_for_tistory, scrape_for_velog
from fastapi_worker.app.services.scraper.configs import TARGET_TAGS, headers

logger = logging.getLogger(__name__)

def scrape_blog_text(url: str) -> Optional[Dict[str, None]]:
    try:
        logger.info(f"[{url}] 웹페이지 스크래핑 요청을 시작합니다.")

        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        
        domain = urlparse(url).netloc.lower()
        text_clean = ""
        
        # Velog 맞춤형 스크래핑 
        if 'velog.io' in domain:
            logger.info(f"[VELOG] 플랫폼 감지 - 맞춤형 파서 실행")
            text_clean = scrape_for_velog(soup)
            
        # Tistory 맞춤형 스크래핑 
        elif 'tistory.com' in domain:
            logger.info(f"[TISTORY] 플랫폼 감지 - 맞춤형 파서 실행")
            text_clean = scrape_for_tistory(soup)

        # 그 외 일반 블로그 공통 스크래핑 
        else:
            logger.info(f"[일반 블로그] 플랫폼 감지 - 공통 파서 실행")
            text_clean = scrape_for_else(soup)

        logger.info(f"[{url}] 스크래핑 완료! 총 {len(text_clean)}자 추출됨.")
        return text_clean

    except Exception as e:
        logger.exception(f"[{url}] 스크래핑 중 오류가 발생했습니다.")
        return None