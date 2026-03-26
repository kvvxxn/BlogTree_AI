package com.navigator.knowledge.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5)) // 서버와 연결하는 데 최대 5초 대기
                .setReadTimeout(Duration.ofSeconds(5))    // 데이터를 읽어오는 데 최대 5초 대기
                .build();
    }
}
