import re
from bs4 import BeautifulSoup

from fastapi_worker.app.services.scraper.configs import TARGET_TAGS

def clean_and_format_html(html_element):
    """
    HTML 요소에서 불필요한 줄바꿈을 방지하고 가독성 좋은 텍스트로 변환합니다.
    """
    # <br> 태그를 실제 줄바꿈 문자로 변환
    for br in html_element.find_all("br"):
        br.replace_with("\n")
        
    # 텍스트는 유지하고 inline 태그 제거 
    inline_tags = ['span', 'strong', 'b', 'i', 'em', 'u', 'a', 'code', 'mark']
    for tag in html_element.find_all(inline_tags):
        tag.unwrap() # 태그만 벗겨내어 주변 텍스트와 자연스럽게 합침
        
    # 줄바꿈 기준으로 텍스트 추출
    text = html_element.get_text(separator='\n', strip=True)
    
    # 띄어쓰기 및 줄바꿈 교정
    text_clean = re.sub(r'\n{3,}', '\n\n', text)

    text_clean = re.sub(r'[ \t]+', ' ', text_clean)

    return text_clean.strip()

def is_nested_target(tag) -> bool:
    """부모 태그가 이미 추출 대상(TARGET_TAGS)에 포함되어 있는지 확인하여 중복 추출 방지"""
    return any(p.name in TARGET_TAGS for p in tag.parents)

def format_tag_text(tag_name: str, text: str) -> str:
    """태그 종류에 따른 가독성(마크다운) 포맷팅 적용"""
    if tag_name == 'pre':
        return f"```\n{text}\n```"
    elif tag_name in ['h1', 'h2', 'h3', 'h4']:
        return f"\n▶ [{text}]"
    elif tag_name == 'blockquote':
        return f"  > {text}"
    elif tag_name == 'li':
        return f"  • {text}"
    return text

def remove_unwanted_tags(content_element, extra_targets=None):
    """script, style 등 불필요한 공통 태그 및 추가 타겟 제거"""
    remove_targets = ['script', 'style', 'header', 'footer', 'nav', 'aside']
    
    if extra_targets:
        remove_targets.extend(extra_targets)
        
    for target in remove_targets:
        if isinstance(target, str):
            for tag in content_element.find_all(target):
                tag.extract()
        else:
            for tag in content_element.find_all(attrs=target):
                tag.extract()