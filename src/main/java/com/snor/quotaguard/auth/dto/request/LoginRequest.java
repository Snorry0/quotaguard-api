package com.snor.quotaguard.auth.dto.request;

import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials for authenticating an existing user.
 */
@Schema(
        description = """
                Credentials for authenticating an existing user. A successful login returns an access
                token (JWT) plus the authenticated user's profile.
                """
)
public record LoginRequest(
        @Schema(
                description = "Registered email address, normalized (trimmed, lowercase).",
                example = "demo@example.com"
        )
        @NotBlank @NormalizedEmail String email,
        @Schema(
                description = "Plain-text password.",
                example = "Password123!"
        )
        @NotBlank String password
) {
}
