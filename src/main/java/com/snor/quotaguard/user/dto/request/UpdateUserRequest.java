package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import com.snor.quotaguard.validation.annotation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateUserRequest(
        @Schema(description = "Optional new email. Only provided fields are updated.")
        @NormalizedEmail
        String email,

        @StrongPassword
        String password,

        Role role
) {
}
