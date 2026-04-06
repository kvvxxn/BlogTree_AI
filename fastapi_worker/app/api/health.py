import logging
from fastapi import APIRouter
from typing import Dict

logger = logging.getLogger(__name__)

router = APIRouter()

@router.get("/ping", summary="Health Check")
async def health_check() -> Dict[str, str]:
    """
    서버 및 API의 상태를 확인

    params: None

    return: JSON 응답으로 "status": "ok"와 "message": "pong"을 포함
    """
    logger.info("[Health Check] Ping 요청 수신 및 처리 완료")
    
    return {
        "status": "ok",
        "message": "pong"
    }