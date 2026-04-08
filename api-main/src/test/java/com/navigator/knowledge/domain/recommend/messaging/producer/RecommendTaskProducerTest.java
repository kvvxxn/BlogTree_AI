package com.navigator.knowledge.domain.recommend.messaging.producer;

import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskRequestMessage;
import com.navigator.knowledge.global.config.rabbitmq.RabbitRecommendProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendTaskProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitRecommendProperties properties;

    @InjectMocks
    private RecommendTaskProducer recommendTaskProducer;

    @Test
    @DisplayName("추천 요청 메시지를 RabbitMQ의 지정된 exchange와 routingKey로 전송해야 한다")
    void sendTaskRequest_shouldSendMessageToRabbitMq() {
        String expectedExchange = "recommend.exchange";
        String expectedRoutingKey = "recommend.request";

        RabbitRecommendProperties.RoutingKey routingKeyProperties =
            new RabbitRecommendProperties.RoutingKey(expectedRoutingKey, "recommend.response");

        when(properties.exchange()).thenReturn(expectedExchange);
        when(properties.routingKey()).thenReturn(routingKeyProperties);

        RecommendTaskRequestMessage requestMessage = new RecommendTaskRequestMessage(
            "task-12345",
            1L,
            "Backend Developer",
            "2023-12-31T23:59:59",
            Map.of("Backend", Map.of("Spring", java.util.List.of("JPA")))
        );

        recommendTaskProducer.sendTaskRequest(requestMessage);

        ArgumentCaptor<CorrelationData> correlationDataCaptor = ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(
            eq(expectedExchange),
            eq(expectedRoutingKey),
            eq(requestMessage),
            correlationDataCaptor.capture()
        );

        assertThat(correlationDataCaptor.getValue().getId()).isEqualTo(requestMessage.taskId());
    }
}
