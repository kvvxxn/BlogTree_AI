import json
import logging
import tiktoken
from typing import Optional, Dict, Any

from fastapi_worker.app.core.exceptions import LLMAnswerParserFailedError

logger = logging.getLogger(__name__)

def truncate_text_by_token(text: str, model_name: str = "gpt-4o", max_tokens: int = 6000) -> str:
    """
    텍스트의 토큰 수를 계산하고, max_tokens를 초과하면 초과분은 잘라내고 반환합니다.
    """
    try:
        # 모델에 맞는 인코더 로드
        encoding = tiktoken.encoding_for_model(model_name)
    except KeyError:
        # 모델명을 찾지 못하면 기본적으로 gpt-4o의 인코더인 o200k_base 등을 사용
        encoding = tiktoken.get_encoding("cl100k_base")

    # 텍스트를 토큰으로 변환 
    tokens = encoding.encode(text)
    
    # 토큰 개수 확인 및 초과시 # max_tokens 만큼만 슬라이싱
    if len(tokens) > max_tokens:
        truncated_text = encoding.decode(tokens[:max_tokens])
        return truncated_text
    
    # 제한을 넘지 않으면 원본 텍스트 반환
    return text


def safe_parse_json(raw_content: str, schema_type: str = "summary") -> Dict[str, Any]:
    """
    LLM의 JSON 응답을 파싱하고, 지정된 스키마(schema_type)에 맞게 검증합니다.
    schema_type: "summary" 또는 "recommend"
    실패 시 LLMAnswerParserFailedError 예외를 발생시킵니다.
    """
    if not raw_content:
        error_msg = "LLM 응답이 비어있습니다."
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # 마크다운 코드 스니펫 제거
    clean_content = raw_content.strip()
    
    if clean_content.startswith("```json"):
        clean_content = clean_content[7:]
    elif clean_content.startswith("```"):
        clean_content = clean_content[3:]
    
    if clean_content.endswith("```"):
        clean_content = clean_content[:-3]
        
    clean_content = clean_content.strip()

    # 우선 JSON 파싱 시도
    try:
        parsed_data = json.loads(clean_content)
    except json.JSONDecodeError as e:
        error_msg = f"JSON 파싱 에러 (잘못된 형식): {e}"
        logger.error(f"{error_msg}\n원본 텍스트: {raw_content}")
        raise LLMAnswerParserFailedError(error_msg)

    # 파싱된 결과가 딕셔너리인지 기본 검증
    if not isinstance(parsed_data, dict):
        error_msg = "파싱된 결과가 JSON 객체(딕셔너리)가 아닙니다."
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # --- 스키마 타입에 따른 필수 필드 검증 ---
    if schema_type == "summary":
        if "summary" not in parsed_data or "keywords" not in parsed_data:
            error_msg = "[Summary 스키마 오류] 필수 키('summary' 또는 'keywords')가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
        if not isinstance(parsed_data["keywords"], list):
            error_msg = "[Summary 스키마 오류] 'keywords'의 값이 리스트 형태가 아닙니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
            
    elif schema_type == "recommend":
        if "recommend_reason" not in parsed_data or "knowledge_tree" not in parsed_data:
            error_msg = "[Recommend 스키마 오류] 필수 키('recommend_reason' 또는 'knowledge_tree')가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
            
        kt_data = parsed_data["knowledge_tree"]
        if not isinstance(kt_data, dict):
            error_msg = "[Recommend 스키마 오류] 'knowledge_tree'의 값이 객체(딕셔너리) 형태가 아닙니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
            
        # knowledge_tree 내부 필수 키 검증
        required_kt_keys = ["category", "topic", "keyword"]
        if not all(key in kt_data for key in required_kt_keys):
            error_msg = f"[Recommend 스키마 오류] 'knowledge_tree' 내부에 필수 키({required_kt_keys}) 중 일부가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)

    else:
        error_msg = f"알 수 없는 스키마 타입입니다: {schema_type}"
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # 모든 검증을 통과하면 안전한 데이터 반환
    return parsed_data