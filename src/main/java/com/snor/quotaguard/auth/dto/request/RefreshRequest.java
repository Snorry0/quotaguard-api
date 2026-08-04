package com.snor.quotaguard.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for rotating a refresh token.
 */
@Schema(
        description = "Refresh token used to obtain a new access token + refresh token pair."
)
public record RefreshRequest(
        @Schema(
                description = "Refresh token value returned from a previous login or refresh.",
                example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2VuLXZhbHVl"
        )
        @NotBlank String refreshToken
) {
}
