# TODO: 생성된 RabbitMQ URL과 큐 이름을 환경 변수로 관리하도록 수정
class Settings:
    RABBITMQ_URL: str = "amqp://guest:guest@localhost:5672/"
    REC_INPUT_QUEUE: str = "recommend_input_queue"
    REC_OUTPUT_QUEUE: str = "recommend_output_queue"
    SUM_INPUT_QUEUE: str = "summary_input_queue"
    SUM_OUTPUT_QUEUE: str = "summary_output_queue"

settings = Settings()