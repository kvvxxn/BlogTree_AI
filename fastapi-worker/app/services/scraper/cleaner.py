# services/scraper/cleaner.py

def clean_soup_to_text(soup):
    if not soup:
        return None
        
    try:
        # 블로그 본문에서 불필요한 태그 제거 (스크립트, 스타일 등)
        for script in soup(["script", "style", "header", "footer", "nav"]):
            script.extract()
            
        text = soup.get_text(separator='\n')
        
        # 빈 줄 제거 등 간단한 전처리
        lines = (line.strip() for line in text.splitlines())
        chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
        text_clean = '\n'.join(chunk for chunk in chunks if chunk)
        
        return text_clean

    except Exception as e:
        print(f"텍스트 전처리 중 오류 발생: {e}")
        return None