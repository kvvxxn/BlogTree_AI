package com.navigator.knowledge.domain.summary.messaging.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.global.config.rabbitmq.RabbitSummaryProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class SummaryTaskProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitSummaryProperties properties;

    @InjectMocks
    private SummaryTaskProducer summaryTaskProducer;

    @Test
    @DisplayName("요청 메시지를 RabbitMQ의 지정된 exchange와 routingKey로 전송해야 한다")
    void sendTaskRequest_ShouldSendMessageToRabbitMQ() {
        // given
        String expectedExchange = "summary.exchange";
        String expectedRoutingKey = "summary.routing.request";
        
        RabbitSummaryProperties.RoutingKey routingKeyProperties = 
                new RabbitSummaryProperties.RoutingKey(expectedRoutingKey, "summary.routing.response");
        
        when(properties.exchange()).thenReturn(expectedExchange);
        when(properties.routingKey()).thenReturn(routingKeyProperties);

        SummaryTaskRequestMessage requestDto = new SummaryTaskRequestMessage(
            "task-12345",
            1L,
            "Backend Developer",
            "https://example.com/article",
            "2023-12-31T23:59:59Z",
            Map.of()
        );

        // when
        summaryTaskProducer.sendTaskRequest(requestDto);

        // then
        ArgumentCaptor<CorrelationData> correlationDataCaptor = ArgumentCaptor.forClass(CorrelationData.class);
        
        verify(rabbitTemplate).convertAndSend(
            eq(expectedExchange),
            eq(expectedRoutingKey),
            eq(requestDto),
            correlationDataCaptor.capture()
        );

        CorrelationData capturedCorrelationData = correlationDataCaptor.getValue();
        assertThat(capturedCorrelationData.getId()).isEqualTo(requestDto.taskId());
    }
}
