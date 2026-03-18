package com.navigator.knowledge.domain.task.dto.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTaskRequest(TaskRequestDto requestDto) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE_NAME,
            RabbitConfig.REQUEST_ROUTING_KEY,
            requestDto
        );

        System.out.println("메시지 발행 성공: " + requestDto.taskId());
    }
}