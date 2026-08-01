package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Schema(description = "Optional new email. Only provided fields are updated.")
        @Email
        String email,

        @Size(min = 8, max = 100)
        String password,

        Role role
) {
}
