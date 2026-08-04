package com.snor.quotaguard.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for revoking a refresh token on logout.
 */
@Schema(
        description = "Refresh token to revoke on logout."
)
public record LogoutRequest(
        @Schema(
                description = "Refresh token value to revoke.",
                example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2VuLXZhbHVl"
        )
        @NotBlank String refreshToken
) {
}
