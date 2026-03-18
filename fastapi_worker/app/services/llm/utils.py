import json
import logging
import tiktoken

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

import json
import logging
from typing import Optional

def safe_parse_json(raw_content: str, schema_type: str = "summary") -> Optional[dict]:
    """
    LLM의 JSON 응답을 안전하게 파싱하고, 지정된 스키마(schema_type)에 맞게 검증합니다.
    schema_type: "summary" 또는 "recommend"
    실패 시 None을 반환합니다.
    """
    if not raw_content:
        logging.error("LLM 응답이 비어있습니다.")
        return None

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
        logging.error(f"JSON 파싱 에러 (잘못된 형식): {e}\n원본 텍스트: {raw_content}")
        return None

    # 파싱된 결과가 딕셔너리인지 기본 검증
    if not isinstance(parsed_data, dict):
        logging.error("파싱된 결과가 JSON 객체(딕셔너리)가 아닙니다.")
        return None

    # --- 스키마 타입에 따른 필수 필드 검증 ---
    if schema_type == "summary":
        if "summary" not in parsed_data or "keywords" not in parsed_data:
            logging.error("[Summary 스키마 오류] 필수 키('summary' 또는 'keywords')가 누락되었습니다.")
            return None
        if not isinstance(parsed_data["keywords"], list):
            logging.error("[Summary 스키마 오류] 'keywords'의 값이 리스트 형태가 아닙니다.")
            return None
            
    elif schema_type == "recommend":
        if "recommend_reason" not in parsed_data or "knowledge_tree" not in parsed_data:
            logging.error("[Recommend 스키마 오류] 필수 키('recommend_reason' 또는 'knowledge_tree')가 누락되었습니다.")
            return None
            
        kt_data = parsed_data["knowledge_tree"]
        if not isinstance(kt_data, dict):
            logging.error("[Recommend 스키마 오류] 'knowledge_tree'의 값이 객체(딕셔너리) 형태가 아닙니다.")
            return None
            
        # knowledge_tree 내부 필수 키 검증
        required_kt_keys = ["category", "topic", "keyword"]
        if not all(key in kt_data for key in required_kt_keys):
            logging.error(f"[Recommend 스키마 오류] 'knowledge_tree' 내부에 필수 키({required_kt_keys}) 중 일부가 누락되었습니다.")
            return None

    else:
        logging.error(f"알 수 없는 스키마 타입입니다: {schema_type}")
        return None

    # 모든 검증을 통과하면 안전한 데이터 반환
    return parsed_data