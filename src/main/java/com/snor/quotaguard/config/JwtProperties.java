package com.snor.quotaguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        Duration refreshExpiration
) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("security.jwt.secret must be at least 32 characters long");
        }
        if (expiration == null) {
            expiration = Duration.ofMinutes(15);
        }
        if (refreshExpiration == null) {
            refreshExpiration = Duration.ofDays(30);
        }
    }
}
