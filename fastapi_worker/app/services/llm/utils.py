import json
import logging
import tiktoken
from typing import Optional, Dict, Any
from langfuse import observe

from fastapi_worker.app.core.exceptions import LLMAnswerParserFailedError, LLMAnswerFailedError

logger = logging.getLogger(__name__)

@observe(name="Check # Tokens and Truncate Text if Exceeds Limit")
def truncate_text_by_token(text: str, model_name: str = "gpt-4o", max_tokens: int = 6000) -> str:
    """
    주어진 모델에 맞는 Encoding 모델을 통해 스크래핑된 블로그 텍스트의 토큰 수를 계산하고, 
    max_tokens를 초과하면 초과분은 잘라내고 반환

    params:
    - text: 토큰 수를 계산하고 필요한 경우 자를 원본 텍스트
    - model_name: 사용할 LLM 모델명 
    - max_tokens: 허용할 최대 Input 토큰 수 

    return: 토큰 수가 max_tokens를 초과하는 경우 자른 텍스트, 그렇지 않으면 원본 텍스트 반환
    - 인코딩 및 토큰화 실패 시 LLMAnswerFailedError 발생
    """
    try:
        try:
            # 모델에 맞는 인코더 로드
            encoding = tiktoken.encoding_for_model(model_name)
        except KeyError:
            # 모델명을 찾지 못하면 기본적으로 gpt-4o의 인코더인 cl100k_base 등을 사용
            encoding = tiktoken.get_encoding("cl100k_base")

        # Texts -> Tokens
        tokens = encoding.encode(text)
        original_token_count = len(tokens)
        
        # 토큰 개수 확인 및 초과시 # max_tokens 만큼만 슬라이싱
        if original_token_count > max_tokens:
            logger.info(f"[Token Truncate] 원본 텍스트의 토큰 수({original_token_count}개)가 제한({max_tokens}개)을 초과하여 텍스트를 자릅니다.")
            truncated_text = encoding.decode(tokens[:max_tokens])
            logger.info(f"[Token Truncate] 텍스트가 {max_tokens}개의 토큰으로 성공적으로 수정되었습니다.")
            return truncated_text
        
        # 제한을 넘지 않으면 원본 텍스트 반환
        logger.info(f"[Token Truncate] 원본 텍스트의 토큰 수({original_token_count}개)가 제한({max_tokens}개) 이내이므로 수정 없이 통과합니다.")
        return text

    except Exception as e:
        # tiktoken 내부 에러, 타입 에러 등 토큰화 과정의 모든 예외 상황을 LLM 실패로 간주
        error_msg = f"[Token Truncate] LLM 분석 전 Encoding 모델을 통한 Token 개수 계산 실패: {e}"
        logger.error(error_msg)
        raise LLMAnswerFailedError(error_msg)


@observe(name="LLM Answer JSON Parsing Attempt")
def safe_parse_json(raw_content: str, schema_type: str = "summary") -> Dict[str, Any]:
    """
    LLM의 JSON 응답을 파싱하고, 지정된 스키마(schema_type)에 맞게 검증합니다.
    schema_type: "summary" 또는 "recommend"

    params:
    - raw_content: LLM이 생성한 원본 텍스트 응답 
    - schema_type: 파싱할 JSON의 스키마 유형 (예: "summary", "recommend") 
        - Task에 따라 필요한 키 값이 다름
    
    return: 파싱된 JSON 데이터
    - 실패 시 LLMAnswerParserFailedError 예외를 발생시킵니다.
    """
    logger.info(f"[JSON Parse] '{schema_type}' 스키마 타입으로 파싱 및 검증을 시작합니다.")

    if not raw_content:
        error_msg = "LLM 응답이 비어있습니다."
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # LLM 응답에서 Markdown 코드 블록 제거 및 앞뒤 공백 제거
    clean_content = raw_content.strip()
    
    if clean_content.startswith("```json"):
        clean_content = clean_content[7:]
    elif clean_content.startswith("```"):
        clean_content = clean_content[3:]
    
    if clean_content.endswith("```"):
        clean_content = clean_content[:-3]
        
    clean_content = clean_content.strip()

    # JSON Parsing
    try:
        parsed_data = json.loads(clean_content)
    except json.JSONDecodeError as e:
        error_msg = f"JSON 파싱 에러 (잘못된 형식): {e}"
        logger.error(f"{error_msg}\n원본 텍스트: {raw_content}")
        raise LLMAnswerParserFailedError(error_msg)

    # 파싱된 데이터가 딕셔너리 형태인지 검증
    if not isinstance(parsed_data, dict):
        error_msg = "파싱된 결과가 JSON 객체(딕셔너리)가 아닙니다."
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # Task에 따라 필요한 키가 존재하는지 검증
    # - Schema type이 잘못되었을 경우 LLMAnswerParserFailedError 발생
    if schema_type == "summary": # Summarize
        if "summary" not in parsed_data or "keywords" not in parsed_data: # 필수 키 검증
            error_msg = "[Summary 스키마 오류] 필수 키('summary' 또는 'keywords')가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
        if not isinstance(parsed_data["keywords"], list): # keywords가 리스트 형태인지 검증
            error_msg = "[Summary 스키마 오류] 'keywords'의 값이 리스트 형태가 아닙니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg) 
            
    elif schema_type == "recommend": # Recommend
        if "recommend_reason" not in parsed_data or "knowledge_tree" not in parsed_data: # 필수 키 검증
            error_msg = "[Recommend 스키마 오류] 필수 키('recommend_reason' 또는 'knowledge_tree')가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
            
        kt_data = parsed_data["knowledge_tree"] 
        if not isinstance(kt_data, dict): # knowledge_tree가 딕셔너리 형태인지 검증
            error_msg = "[Recommend 스키마 오류] 'knowledge_tree'의 값이 객체(딕셔너리) 형태가 아닙니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)
            
        # knowledge_tree 내부 필수 키 검증
        required_kt_keys = ["category", "topic", "keyword"]
        if not all(key in kt_data for key in required_kt_keys):
            error_msg = f"[Recommend 스키마 오류] 'knowledge_tree' 내부에 필수 키({required_kt_keys}) 중 일부가 누락되었습니다."
            logger.error(error_msg)
            raise LLMAnswerParserFailedError(error_msg)

    else: # Schema type 오류
        error_msg = f"알 수 없는 스키마 타입입니다: {schema_type}"
        logger.error(error_msg)
        raise LLMAnswerParserFailedError(error_msg)

    # 파싱 및 스키마 검증 성공 로깅
    logger.info(f"[JSON Parse] '{schema_type}' 스키마에 맞게 파싱 및 검증이 성공적으로 완료되었습니다.")
    
    return parsed_data