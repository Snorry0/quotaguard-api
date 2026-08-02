package com.snor.quotaguard.auth.dto.request;

import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import com.snor.quotaguard.validation.annotation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Schema(
                description = "Unique email address used as login identifier.",
                example = "demo@example.com"
        )
        @NormalizedEmail
        @NotBlank
        String email,

        @Schema(
                description = "Plain-text password. It is hashed before persistence.",
                example = "Password123!",
                minLength = 8
        )
        @NotBlank
        @StrongPassword
        String password
) {
}
