package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import com.snor.quotaguard.validation.annotation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NormalizedEmail
        @NotBlank
        String email,

        @NotBlank
        @StrongPassword
        String password,

        @Schema(description = "Assigned role. Defaults to USER when omitted.")
        Role role
) {
}
