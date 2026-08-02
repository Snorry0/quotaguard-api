package com.snor.quotaguard.audit.dto.response;

import com.snor.quotaguard.audit.domain.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * A single audit trail event.
 */
@Schema(
        description = """
                A single audit trail event. `action` is one of the values documented on the
                `AuditAction` enum. `success` indicates whether the underlying operation completed
                successfully.
                """
)
public record AuditEventResponse(
        @Schema(
                description = "Unique identifier of the audit event.",
                example = "8f90a1b2-4e5f-6a7b-9c0d-1234567890ef"
        )
        UUID id,
        @Schema(
                description = "Timestamp (ISO 8601) at which the event occurred.",
                example = "2026-08-02T10:15:30.123456Z"
        )
        Instant timestamp,
        @Schema(
                description = "Identifier of the user who triggered the event. Null for system-triggered events.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID actorId,
        @Schema(
                description = "Email address of the user who triggered the event.",
                example = "demo@example.com"
        )
        String actorEmail,
        @Schema(example = "LOGIN_SUCCESS")
        AuditAction action,
        @Schema(
                description = "Type of resource the event relates to.",
                example = "USER"
        )
        String resourceType,
        @Schema(
                description = "Identifier of the resource the event relates to.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID resourceId,
        @Schema(
                description = "Human-readable description of the event.",
                example = "User logged in successfully"
        )
        String description,
        @Schema(
                description = "Client IP address the event originated from.",
                example = "203.0.113.42"
        )
        String ipAddress,
        @Schema(
                description = "Whether the underlying operation completed successfully.",
                example = "true"
        )
        boolean success
) {
}
