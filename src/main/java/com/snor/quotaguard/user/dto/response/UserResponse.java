package com.snor.quotaguard.user.dto.response;

import com.snor.quotaguard.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public representation of a user account.
 */
@Schema(
        description = """
                Public representation of a user account. `role` is one of the values documented on the
                `Role` enum.
                """
)
public record UserResponse(
        @Schema(
                description = "Unique identifier of the user.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID id,
        @Schema(
                description = "Email address used as login identifier (normalized).",
                example = "demo@example.com"
        )
        String email,
        @Schema(example = "USER")
        Role role,
        @Schema(
                description = "Timestamp (ISO 8601) at which the account was created.",
                example = "2026-08-01T10:00:00"
        )
        LocalDateTime createdAt
) {
}
