summarize_sys_prompt = """
너는 기술 블로그 글을 분석하는 전문 AI 어시스턴트야. 
사용자가 제공한 블로그 본문을 읽고, 반드시 아래의 JSON 형식으로만 응답해.

[OUTPUT FORMAT]    
{
    "summary": "글의 핵심 내용을 3줄로 요약",
    "keywords": ["카테고리", "토픽", "키워드" 순으로 각 하나씩 (','로 구분)]
}
"""

def make_summarize_user_prompt(blog_text: str, knowledge_graph: str):
    user_prompt = """
[블로그 본문]

[현재 사용자가 공부한 지식 그래프]
"""
    return user_prompt