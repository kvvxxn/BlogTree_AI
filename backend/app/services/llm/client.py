# services/llm/client.py
import os
import json
from openai import OpenAI
from .prompts import SYSTEM_PROMPT

def analyze_text(text):
    print("LLM을 통해 본문 분석 및 추천을 생성합니다...")
    api_key = os.getenv("OPENAI_API_KEY")
    client = OpenAI(api_key=api_key)
    
    # 토큰 제한을 고려하여 앞부분 4000자 정도만 자르기 (필요시 조절)
    truncated_text = text[:4000] 

    try:
        response = client.chat.completions.create(
            model="gpt-5.1", 
            response_format={ "type": "json_object" },
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": f"다음 블로그 글을 분석해줘:\n\n{truncated_text}"}
            ],
            temperature=0.3
        )
        
        # JSON 문자열을 파이썬 딕셔너리로 변환
        result_json = json.loads(response.choices[0].message.content)
        return result_json

    except Exception as e:
        print(f"LLM API 호출 중 오류 발생: {e}")
        return None