import tiktoken

def truncate_text_by_token(text: str, model_name: str = "gpt-4o", max_tokens: int = 6000) -> str:
    """
    텍스트의 토큰 수를 계산하고, max_tokens를 초과하면 초과분은 잘라내고 반환합니다.
    """
    try:
        # 1. 모델에 맞는 인코더 가져오기
        encoding = tiktoken.encoding_for_model(model_name)
    except KeyError:
        # 모델명을 찾지 못하면 기본적으로 gpt-4o의 인코더인 o200k_base 등을 사용하도록 폴백(Fallback)
        encoding = tiktoken.get_encoding("cl100k_base")

    # 2. 텍스트를 토큰으로 변환 (인코딩)
    tokens = encoding.encode(text)
    
    # 3. 토큰 개수 확인 및 절삭
    if len(tokens) > max_tokens:
        # max_tokens 만큼만 슬라이싱한 후 다시 텍스트로 변환 (디코딩)
        truncated_text = encoding.decode(tokens[:max_tokens])
        return truncated_text
    
    # 제한을 넘지 않으면 원본 텍스트 반환
    return text