package com.navigator.knowledge.domain.summary.messaging.producer;

import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.global.config.rabbitmq.RabbitSummaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryTaskProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitSummaryProperties properties;

    public void sendTaskRequest(SummaryTaskRequestMessage requestDto) {
        CorrelationData correlationData = new CorrelationData(requestDto.taskId());

        rabbitTemplate.convertAndSend(
            properties.exchange(),
            properties.routingKey().request(),
            requestDto,
            correlationData
        );
    }
}
