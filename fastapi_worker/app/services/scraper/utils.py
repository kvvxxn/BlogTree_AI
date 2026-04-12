import re
from bs4 import BeautifulSoup

from fastapi_worker.app.services.scraper.configs import TARGET_TAGS

def clean_and_format_html(html_element: BeautifulSoup) -> str:
    """
    HTML 요소에서 불필요한 줄바꿈을 방지하고 가독성 좋은 텍스트로 변환

    params:
    - html_element: BeautifulSoup 객체로 파싱된 HTML 요소

    return: 가공된 텍스트
    """
    for br in html_element.find_all("br"):
        br.replace_with("\n") # <br> -> 줄바꿈
        
    inline_tags = ['span', 'strong', 'b', 'i', 'em', 'u', 'a', 'code', 'mark']
    for tag in html_element.find_all(inline_tags):
        tag.unwrap() # Inline Tag 제거
        
    text = html_element.get_text(separator='\n', strip=True) # 줄바꿈 기준으로 텍스트 추출
    
    text_clean = re.sub(r'\n{3,}', '\n\n', text) # 띄어쓰기 및 줄바꿈 교정

    text_clean = re.sub(r'[ \t]+', ' ', text_clean)

    return text_clean.strip()

def is_nested_target(tag: BeautifulSoup) -> bool:
    """
    부모 태그가 이미 추출 대상(TARGET_TAGS)에 포함되어 있는지 확인하여 중복 추출 방지
    
    params:
    - tag: BeautifulSoup 객체로 파싱된 HTML 요소
    
    return: 중복 추출 여부
    """

    return any(p.name in TARGET_TAGS for p in tag.parents)

def format_tag_text(tag_name: str, text: str) -> str:
    """
    태그 종류에 따른 가독성(마크다운) 포맷팅 적용

    params:
    - tag_name: HTML 태그명
    - text: 태그에서 추출한 텍스트

    return: 포맷팅이 적용된 텍스트
    """

    if tag_name == 'pre':
        return f"```\n{text}\n```"
    elif tag_name in ['h1', 'h2', 'h3', 'h4']:
        return f"\n▶ [{text}]"
    elif tag_name == 'blockquote':
        return f"  > {text}"
    elif tag_name == 'li':
        return f"  • {text}"
    return text

def remove_unwanted_tags(content_element: BeautifulSoup, extra_targets: list = None) -> None:
    """
    script, style 등 불필요한 공통 태그 및 추가 타겟 제거

    params:
    - content_element: BeautifulSoup 객체로 파싱된 HTML 요소 
    - extra_targets: 추가로 제거할 태그 리스트 

    return: None
    - 원본 content_element에서 불필요한 태그만 제거
    """

    if content_element is None:
        return
    
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