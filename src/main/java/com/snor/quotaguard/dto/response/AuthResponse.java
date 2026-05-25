package com.snor.quotaguard.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_at") Instant expiresAt,
        UserResponse user
) {
    public AuthResponse(String accessToken, Instant expiresAt, UserResponse user) {
        this(accessToken, "Bearer", expiresAt, user);
    }
}
