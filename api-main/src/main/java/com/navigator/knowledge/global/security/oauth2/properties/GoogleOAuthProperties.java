package com.navigator.knowledge.global.security.oauth2.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oauth2.google")
public record GoogleOAuthProperties (
    String clientId,
    String clientSecret,
    String redirectUri,
    String tokenUri,
    String userInfoUri
) {}