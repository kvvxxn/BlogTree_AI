from fastapi import APIRouter

router = APIRouter()

@router.get("/ping", summary="Health Check")
async def health_check():
    """
    서버 및 API의 상태를 확인
    """
    return {
        "status": "ok",
        "message": "pong"
    }