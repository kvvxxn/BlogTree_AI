import os
import json
from openai import OpenAI
from dotenv import load_dotenv
from typing import Optional, Dict, Any

from fastapi_worker.app.services.llm.prompts import summarize_sys_prompt, make_summarize_user_prompt
from fastapi_worker.app.services.llm.configs import T, OUTPUT_MAX_TOKENS, INPUT_MAX_TOKENS
from fastapi_worker.app.services.llm.utils import truncate_text_by_token, safe_parse_summary_json

# LLM API Key 로드
load_dotenv() 
api_key = os.getenv("OPENAI_API_KEY")

def analyze_with_llm(blog_text: str, knowledge_graph: str) -> Optional[Dict[str, None]]:
    client = OpenAI(api_key=api_key)
    
    # 본문을 토큰화하여 최대 토큰을 초과하는지 확인 후 초과 시 자르기
    truncated_text = truncate_text_by_token(
        text=blog_text, 
        model_name="gpt-4o", 
        max_tokens=INPUT_MAX_TOKENS
    )

    # 스크래핑한 결과와 Knowledge Graph를 바탕으로 User prompt 생성
    summarize_user_prompt = make_summarize_user_prompt(
        blog_text=truncated_text, 
        knowledge_graph=knowledge_graph
    )

    try:
        # LLM 응답 생성
        response = client.chat.completions.create(
            model="gpt-4o", 
            response_format={ "type": "json_object" },
            messages=[
                {"role": "system", "content": summarize_sys_prompt},
                {"role": "user", "content": summarize_user_prompt},
            ],
            temperature=T,
            max_tokens=OUTPUT_MAX_TOKENS
        )
        
        # 안전하게 JSON 파싱
        result_json = safe_parse_summary_json(
            raw_content=response
        )

        if result_json is None:
            print("LLM 응답 파싱에 실패했습니다.")
            return None

    except Exception as e:
        print(f"❌ LLM API 호출 중 오류 발생: {e}")
        return None