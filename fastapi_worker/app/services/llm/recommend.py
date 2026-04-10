import os
import json
import logging
from dotenv import load_dotenv
from typing import Optional, Dict, Any
from langfuse import observe
from langfuse.openai import OpenAI

from fastapi_worker.app.services.llm.prompts import recommend_sys_prompt, make_recommend_user_prompt
from fastapi_worker.app.services.llm.configs import T, OUTPUT_MAX_TOKENS, MODEL_NAME
from fastapi_worker.app.core.exceptions import LLMAnswerFailedError, LLMAnswerParserFailedError
from fastapi_worker.app.services.llm.utils import safe_parse_json
from fastapi_worker.app.core.config import settings

logger = logging.getLogger(__name__)

@observe(name="Recommendation using LLM")
def recommend_with_llm(career_goal: str, knowledge_tree: str) -> Dict[str, Any]: 
    """
    LLM을 사용하여 사용자의 커리어 목표와 지식 트리를 바탕으로 맞춤형 키워드 추천 및 분석
    - 사용자의 지식 트리를 기반으로 LLM 답변 생성 그리고 JSON 파싱을 포함하는 Warpper 함수
    
    params:
    - career_goal: 사용자의 커리어 목표 
    - knowledge_tree: 사용자가 현재까지 구축한 지식 트리 정보 

    return: LLM이 생성한 요약 및 분석 결과 (JSON)
    - LLM 응답이 비어있거나 JSON 파싱 실패 시 LLMAnswerParserFailedError 발생
    - LLM API 호출 실패 시 LLMAnswerFailedError 발생
    """
    client = OpenAI(api_key=settings.LLM_API_KEY)

    logger.info("사용자의 커리어 목표 기반 맞춤형 키워드 추천 분석을 시작합니다.")

    # 사용자 맞춤 정보로 User prompt 생성
    recommend_user_prompt = make_recommend_user_prompt(
        career_goal=career_goal, 
        knowledge_tree=knowledge_tree
    )

    try:
        logger.info(f"OpenAI API({MODEL_NAME})를 호출하여 지식 트리 기반 키워드 추천을 요청합니다...")

        # LLM API 호출
        response = client.chat.completions.create(
            model=MODEL_NAME, 
            response_format={ "type": "json_object" },
            messages=[
                {"role": "system", "content": recommend_sys_prompt},
                {"role": "user", "content": recommend_user_prompt},
            ],
            temperature=T,
            max_tokens=OUTPUT_MAX_TOKENS
        )

        logger.info("OpenAI API 응답을 성공적으로 수신했습니다. 추천 결과 JSON 파싱을 시도합니다.")
        raw_content = response.choices[0].message.content

        # LLM 응답에서 JSON 부분만 안전하게 파싱하여 결과 반환 및 에러 처리
        # - 파싱 실패시 LLMAnswerParserFailedError 발생
        result_json = safe_parse_json(
            raw_content=raw_content,
            schema_type="recommend"
        )

        logger.info("LLM 추천 결과 JSON 파싱 완료!")
        return result_json

    except LLMAnswerParserFailedError:
        # 파싱 실패 에러는 그대로 던짐
        raise
    except Exception as e:
        logger.exception("추천 LLM API 호출 중 예기치 않은 오류가 발생했습니다.")
        raise LLMAnswerFailedError(f"LLM API 호출 실패: {str(e)}")