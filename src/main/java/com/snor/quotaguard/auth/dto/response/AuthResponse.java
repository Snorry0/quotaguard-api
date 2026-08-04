package com.snor.quotaguard.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.snor.quotaguard.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Result of a successful authentication (register or login).
 */
@Schema(
        description = """
                Result of a successful authentication (register, login or refresh): a JWT access token,
                its bearer token type, the expiry instant, the authenticated user's profile and a
                long-lived refresh token. The JSON property names are snake_case (`access_token`,
                `token_type`, `expires_at`, `refresh_token`).
                """
)
public record AuthResponse(
        @JsonProperty("access_token")
        @Schema(
                description = "Signed JWT access token. Send it in the `Authorization` header as `Bearer <token>`. Expires at `expires_at`.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyZjA3YzViMi00ZjBkLTQwOTAtODZjMS0wMjFlNWY2YjgwZjgiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTcyMjUwMDAwMCwiZXhwIjoxNzIyNTAzNjAwfQ.example-signature"
        )
        String accessToken,
        @JsonProperty("token_type")
        @Schema(
                description = "Token type; always `Bearer`.",
                example = "Bearer"
        )
        String tokenType,
        @JsonProperty("expires_at")
        @Schema(
                description = "Instant (ISO 8601) at which the access token expires.",
                example = "2026-08-02T22:15:30Z"
        )
        Instant expiresAt,
        @Schema(
                description = "The authenticated user's profile."
        )
        UserResponse user,
        @JsonProperty("refresh_token")
        @Schema(
                description = "Opaque refresh token (Base64 URL-safe). Use it at `POST /api/v1/auth/refresh` to obtain a new access + refresh pair, or at `POST /api/v1/auth/logout` to revoke.",
                example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2VuLXZhbHVl"
        )
        String refreshToken
) {
    public AuthResponse(String accessToken, Instant expiresAt, UserResponse user) {
        this(accessToken, "Bearer", expiresAt, user, null);
    }

    public AuthResponse(String accessToken, Instant expiresAt, UserResponse user, String refreshToken) {
        this(accessToken, "Bearer", expiresAt, user, refreshToken);
    }
}
