import requests
from bs4 import BeautifulSoup
from urllib.parse import urlparse
from typing import Optional, Dict, Any

from fastapi_worker.app.services.scraper.parsers import scrape_for_else, scrape_for_tistory, scrape_for_tistory, scrape_for_velog
from fastapi_worker.app.services.scraper.configs import TARGET_TAGS, headers

def scrape_blog_text(url: str) -> Optional[Dict[str, None]]:
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        
        domain = urlparse(url).netloc.lower()
        text_clean = ""
        
        # Velog 맞춤형 스크래핑 
        if 'velog.io' in domain:
            text_clean = scrape_for_velog(soup)
            
        # Tistory 맞춤형 스크래핑 
        elif 'tistory.com' in domain:
            text_clean = scrape_for_tistory(soup)

        # 그 외 일반 블로그 공통 스크래핑 
        else:
            text_clean = scrape_for_else(soup)

        return text_clean

    except Exception as e:
        print(f"❌ 스크래핑 중 오류 발생: {e}")
        return None