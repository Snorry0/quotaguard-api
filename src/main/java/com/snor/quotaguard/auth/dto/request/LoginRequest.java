package com.snor.quotaguard.auth.dto.request;

import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @NormalizedEmail String email,
        @NotBlank String password
) {
}
