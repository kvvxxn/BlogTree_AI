package com.navigator.knowledge.global.config.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq.recommend")
public record RabbitRecommendProperties(
    String exchange,
    RecommendQueue queue,
    RoutingKey routingKey
) {
    public record RecommendQueue(
        String request,
        String response
    ) {}

    public record RoutingKey(
        String request,
        String response
    ) {}
}
