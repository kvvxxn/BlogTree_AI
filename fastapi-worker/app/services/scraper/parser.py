# services/scraper/parser.py
import requests
from bs4 import BeautifulSoup

def fetch_soup(url):
    print(f"[{url}] 스크래핑을 시작합니다...")
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.text, 'html.parser')
        return soup

    except Exception as e:
        print(f"스크래핑 중 오류 발생: {e}")
        return None