import aio_pika
from pydantic import BaseModel
from langfuse.decorators import observe

from fastapi_worker.app.core.config import settings

@observe("Summarization Publish")    
async def publish_message(queue_name: str, message_body: BaseModel):
    """
    지정된 큐로 Pydantic 객체를 직렬화하여 전송합니다.
    """
    connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
    
    async with connection:
        channel = await connection.channel()
        
        # JSON 설정 규칙에 맞춘 동적 이름 생성 
        parts = queue_name.split(".")
        prefix = parts[0] # summary
        msg_type = parts[1] # response
        
        exchange_name = f"{prefix}.exchange"              # 결과: summary.exchange
        publish_routing_key = f"{prefix}.{msg_type}"      # 결과: summary.response
        dlx_routing_key = f"{prefix}.{msg_type}.dead"     # 결과: summary.response.dead
        
        # 1. Exchange 선언 
        exchange = await channel.get_exchange(name=exchange_name)
        
        # 2. Queue 선언 
        queue = await channel.declare_queue(
            name=queue_name, 
            durable=True,
            arguments={
                "x-dead-letter-exchange": "dlx.exchange",
                "x-dead-letter-routing-key": dlx_routing_key  
            }
        )
        
        # 3. Queue와 Exchange 바인딩 
        await queue.bind(exchange, routing_key=publish_routing_key)
        
        message = aio_pika.Message(
            # Pydantic 모델을 JSON 문자열로 변환 후 바이트로 인코딩
            body=message_body.model_dump_json().encode("utf-8"),
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT 
        )
        
        # 4. Default exchange가 아닌, 선언된 Topic Exchange를 통해 발행
        await exchange.publish(
            message,
            routing_key=publish_routing_key,
        )
        print(f"[Publisher] 익스체인지 '{exchange_name}' (라우팅 키: '{publish_routing_key}')로 스키마 전송 완료")