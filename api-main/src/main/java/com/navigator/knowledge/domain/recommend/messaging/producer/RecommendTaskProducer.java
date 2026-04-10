package com.navigator.knowledge.domain.recommend.messaging.producer;

import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskRequestMessage;
import com.navigator.knowledge.global.config.rabbitmq.RabbitRecommendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendTaskProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitRecommendProperties properties;

    public void sendTaskRequest(RecommendTaskRequestMessage requestMessage) {
        CorrelationData correlationData = new CorrelationData(requestMessage.taskId());

        rabbitTemplate.convertAndSend(
            properties.exchange(),
            properties.routingKey().request(),
            requestMessage,
            correlationData
        );
    }
}
