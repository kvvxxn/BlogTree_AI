package com.navigator.knowledge.global.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Queue;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "learning.navigator.exchange";
    public static final String REQUEST_QUEUE = "task.request.queue";
    public static final String REQUEST_ROUTING_KEY = "task.request";

    @Bean
    public TopicExchange learningNavigatorExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue requestQueue() {
        return new Queue(REQUEST_QUEUE, true);
    }

    @Bean
    public Binding requestBinding(Queue requestQueue, TopicExchange learningNavigatorExchange) {
        return BindingBuilder.bind(requestQueue)
            .to(learningNavigatorExchange)
            .with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        // Snake Case 대응을 위한 ObjectMapper 설정 (선택 사항)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}