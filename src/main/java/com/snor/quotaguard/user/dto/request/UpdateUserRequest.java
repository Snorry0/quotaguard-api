package com.snor.quotaguard.user.dto.request;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import com.snor.quotaguard.validation.annotation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for partially updating a user account (ADMIN only).
 */
@Schema(
        description = """
                Payload for partially updating a user account (ADMIN only). PATCH semantics: only the
                provided fields are updated. See `Role` for the possible values and their meanings.
                """
)
public record UpdateUserRequest(
        @Schema(
                description = "Optional new email. Only provided fields are updated.",
                example = "updated@example.com",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @NormalizedEmail
        String email,

        @Schema(
                description = "Optional new password. It is hashed before persistence.",
                example = "Password123!"
        )
        @StrongPassword
        String password,

        @Schema(example = "USER")
        Role role
) {
}
