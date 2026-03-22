package com.navigator.knowledge.domain.task.mq.producer;

import com.navigator.knowledge.domain.task.mq.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.global.config.rabbitmq.RabbitSummaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryTaskProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitSummaryProperties properties;

    public void sendTaskRequest(SummaryTaskRequestMessage requestDto) {
        rabbitTemplate.convertAndSend(
            properties.exchange(),
            properties.routingKey().request(),
            requestDto
        );
    }
}