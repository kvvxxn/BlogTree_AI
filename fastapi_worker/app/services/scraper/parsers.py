from bs4 import BeautifulSoup
from fastapi_worker.app.services.scraper.utils import (
    clean_and_format_html, 
    is_nested_target, 
    format_tag_text, 
    remove_unwanted_tags,
    TARGET_TAGS
)

def scrape_for_velog(soup: BeautifulSoup) -> str:
    content_tags = soup.find_all(TARGET_TAGS)
    extracted_lines = []
    
    for tag in content_tags:
        if is_nested_target(tag):
            continue

        text = clean_and_format_html(tag)
        if not text or text in ["로그인", "팔로우", "목록 보기"]:
            continue
                
        # "다음 포스트" 등의 키워드가 포함되어 있으면 하단 크롤링 완전 중단
        if any(keyword in text for keyword in ["관심이 갈 만한 포스트", "이전 포스트", "다음 포스트", "개의 댓글"]):
            break
                    
        # 포맷팅 적용
        formatted_text = format_tag_text(tag.name, text)
        extracted_lines.append(formatted_text)
                
    return '\n\n'.join(extracted_lines).strip()


def scrape_for_tistory(soup: BeautifulSoup) -> str:
    target_classes = ['article_view', 'entry-content', 'tt_article_useless_p_margin']
    main_content = None
    
    for class_name in target_classes:
        content = soup.find('div', class_=class_name)
        if content:
            main_content = content
            break
            
    if not main_content:
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
    
    return '\n\n'.join(extracted_lines).strip()


def scrape_for_else(soup: BeautifulSoup) -> str:
    main_content = soup.find('article') or soup.find('main') or soup.find('body')
    
    # 공통 불필요 태그 제거
    remove_unwanted_tags(main_content)
    
    text_clean = clean_and_format_html(main_content)
    return text_clean.strip()