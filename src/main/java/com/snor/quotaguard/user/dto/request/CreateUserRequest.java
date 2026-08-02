package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import com.snor.quotaguard.validation.annotation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating a user account (ADMIN only).
 */
@Schema(
        description = """
                Payload for creating a user account (ADMIN only). The account is created with the given
                email, password and role. The role defaults to `USER` when omitted; see `Role` for the
                possible values and their meanings.
                """
)
public record CreateUserRequest(
        @Schema(
                description = "Unique email address used as login identifier.",
                example = "newuser@example.com"
        )
        @NormalizedEmail
        @NotBlank
        String email,

        @Schema(
                description = "Plain-text password. It is hashed before persistence.",
                example = "Password123!"
        )
        @NotBlank
        @StrongPassword
        String password,

        @Schema(example = "USER")
        Role role
) {
}
