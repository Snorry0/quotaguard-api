package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "Assigned role. Defaults to USER when omitted.")
        Role role
) {
}
