from pydantic import computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # GPT API Key
    LLM_API_KEY: str = "your_default_api_key_here"

    # RabbiMQ 연결 정보
    RABBITMQ_USER: str = "rabbitmq_user"
    RABBITMQ_PASSWORD: str = "rabbitmq_password"
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    
    @computed_field
    @property
    def RABBITMQ_URL(self) -> str:
        return f"amqp://{self.RABBITMQ_USER}:{self.RABBITMQ_PASSWORD}@{self.RABBITMQ_HOST}:{self.RABBITMQ_PORT}/"
    
    # Exchange 이름 설정
    SUMMARY_EXCHANGE: str = "summary.exchange"
    RECOMMEND_EXCHANGE: str = "recommend.exchange"
    
    # Queue 이름 설정
    SUMMARIZE_INPUT_QUEUE: str = "summary.request.queue"
    SUMMARIZE_OUTPUT_QUEUE: str = "summary.response.queue"
    RECOMMEND_INPUT_QUEUE: str = "recommend.request.queue"
    RECOMMEND_OUTPUT_QUEUE: str = "recommend.response.queue"

    # Routing Key 설정
    SUMMARIZE_INPUT_ROUTING_KEY: str = "summary.request"
    SUMMARIZE_OUTPUT_ROUTING_KEY: str = "summary.response"
    RECOMMEND_INPUT_ROUTING_KEY: str = "recommend.request"
    RECOMMEND_OUTPUT_ROUTING_KEY: str = "recommend.response"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

settings = Settings()