package com.snor.quotaguard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(
                description = "Unique email address used as login identifier.",
                example = "demo@example.com"
        )
        @Email
        @NotBlank
        String email,

        @Schema(
                description = "Plain-text password. It is hashed before persistence.",
                example = "Password123!",
                minLength = 8
        )
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
