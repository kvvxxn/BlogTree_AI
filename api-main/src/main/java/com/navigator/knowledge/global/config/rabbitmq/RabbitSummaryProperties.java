package com.navigator.knowledge.global.config.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq.summary")
public record RabbitSummaryProperties(
    String exchange,
    SummaryQueue queue,
    RoutingKey routingKey
) {
    public record SummaryQueue(
        String request,
        String response
    ) {}

    public record RoutingKey(
        String request,
        String response
    ) {}
}