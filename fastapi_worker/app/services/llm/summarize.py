import os
import json
from openai import OpenAI
from dotenv import load_dotenv

from fastapi_worker.app.services.llm.prompts import summarize_sys_prompt, make_summarize_user_prompt
from fastapi_worker.app.services.llm.configs import T, OUTPUT_MAX_TOKENS, INPUT_MAX_TOKENS
from fastapi_worker.app.services.llm.utils import truncate_text_by_token

# LLM API Key load
load_dotenv() 
api_key = os.getenv("OPENAI_API_KEY")

def analyze_with_llm(blog_text, knowledge_graph):
    client = OpenAI(api_key=api_key)
    
    # 본문을 토큰화하여 최대 토큰을 초과하는지 확인 후 초과 시 자르기
    truncated_text = truncate_text_by_token(
        text=blog_text, 
        model_name="gpt-4o", 
        max_tokens=INPUT_MAX_TOKENS
    )

    summarize_user_prompt = make_summarize_user_prompt(
        blog_text=truncated_text, 
        knowledge_graph=knowledge_graph
    )

    try:
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
        
        # TODO: 안전하게 파싱하는거 추가
        result_json = json.loads(response.choices[0].message.content)
        return result_json

    except Exception as e:
        print(f"❌ LLM API 호출 중 오류 발생: {e}")
        return None