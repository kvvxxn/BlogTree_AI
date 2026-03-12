import os
import json
import logging
from openai import OpenAI
from dotenv import load_dotenv
from typing import Optional, Dict, Any

from fastapi_worker.app.services.llm.prompts import summarize_sys_prompt, make_summarize_user_prompt
from fastapi_worker.app.services.llm.configs import T, OUTPUT_MAX_TOKENS, INPUT_MAX_TOKENS
from fastapi_worker.app.services.llm.utils import truncate_text_by_token, safe_parse_summary_json

logger = logging.getLogger(__name__)

# LLM API Key 로드
load_dotenv() 
api_key = os.getenv("OPENAI_API_KEY")

def analyze_with_llm(url: str, blog_text: str, knowledge_graph: str) -> Optional[Dict[str, Any]]: # None 대신 Any로 힌트 수정
    client = OpenAI(api_key=api_key)

    logger.info(f"[{url}] LLM 분석을 위한 텍스트 전처리 및 프롬프트 생성을 시작합니다.")

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
        # LLM 응답 생성 직전 로그 
        logger.info(f"[{url}] OpenAI API(gpt-4o)를 호출하여 요약 및 분석을 요청합니다...")

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

        logger.info(f"[{url}] LLM API 응답을 성공적으로 수신했습니다. JSON 파싱을 시도합니다.")

        raw_content = response.choices[0].message.content

        # 안전하게 JSON 파싱
        result_json = safe_parse_summary_json(
            raw_content=raw_content
        )

        # JSON 파싱 실패 시
        if result_json is None:
            logger.error(f"[{url}] LLM 응답이 올바른 JSON 형식이 아니어서 파싱에 실패했습니다.")
            return None

        logger.info(f"[{url}] LLM 분석 결과 JSON 파싱 완료!")
        return result_json

    except Exception as e:
        logger.exception(f"[{url}] LLM API 호출 중 예기치 않은 오류가 발생했습니다.")
        return None