from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    LLM_API_KEY: str = "your_default_api_key_here"
    
    # RabbitMQ 설정 (docker-compose 기준)
    # FastAPI를 로컬에서 실행한다면 localhost, Docker 내부라면 rabbitmq를 사용합니다.
    RABBITMQ_URL: str = "amqp://guest:guest@localhost:5672/"
    
    # 큐 이름 설정
    SUMMARIZE_INPUT_QUEUE: str = "task_input_queue"
    SUMMARIZE_OUTPUT_QUEUE: str = "task_output_queue"
    RECOMMEND_INPUT_QUEUE: str = "recommend_input_queue"
    RECOMMEND_OUTPUT_QUEUE: str = "recommend_output_queue"

    class Config:
        env_file = ".env"   

settings = Settings()