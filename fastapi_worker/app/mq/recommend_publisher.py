import aio_pika
from pydantic import BaseModel

from fastapi_worker.app.core.config import settings
    
async def publish_message(queue_name: str, message_body: BaseModel):
    """
    지정된 큐로 Pydantic 객체를 직렬화하여 전송합니다.
    """
    connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
    
    async with connection:
        channel = await connection.channel()
        queue = await channel.declare_queue(queue_name, durable=True)
        
        message = aio_pika.Message(
            # Pydantic 모델을 JSON 문자열로 변환 후 바이트로 인코딩
            body=message_body.model_dump_json().encode("utf-8"),
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT 
        )
        
        await channel.default_exchange.publish(
            message,
            routing_key=queue.name,
        )
        print(f"[Publisher] 큐 '{queue_name}'로 스키마 전송 완료")