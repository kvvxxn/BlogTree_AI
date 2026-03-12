# services/processor.py
import os
from dotenv import load_dotenv

# 분리된 모듈들 임포트
from scraper.parser import fetch_soup
from scraper.cleaner import clean_soup_to_text
from llm.client import analyze_text

# .env 파일 로드 (최상위 실행 파일에서 한 번만 호출해주는 것이 좋습니다)
load_dotenv() 

def process_blog_url(url):
    """URL을 받아 스크래핑, 전처리, LLM 분석을 거치는 파이프라인"""
    # 1. 스크래핑 (HTML 파싱)
    soup = fetch_soup(url)
    if not soup:
        return None

    # 2. 텍스트 전처리
    blog_text = clean_soup_to_text(soup)
    if not blog_text:
        return None

    # 3. LLM 분석
    result = analyze_text(blog_text)
    return result

def main():    
    target_url = input("블로그 URL을 입력하세요: ") 
    
    result = process_blog_url(target_url)
    
    if result:
        print("\n=== 🎯 분석 결과 ===")
        print(f"📝 요약:\n{result.get('summary')}\n")
        print(f"🔑 핵심 키워드: {', '.join(result.get('keywords', []))}\n")
        print("🚀 다음 학습 추천:")
        for item in result.get('next_topics', []):
            print(f"  - {item['topic']} ({item['reason']})")

if __name__ == "__main__":
    main()