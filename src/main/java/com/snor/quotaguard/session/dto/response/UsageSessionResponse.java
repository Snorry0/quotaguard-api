package com.snor.quotaguard.session.dto.response;

import com.snor.quotaguard.domain.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A usage session, active or completed.
 */
@Schema(
        description = """
                A usage session, active or completed. `status` is one of the values documented on the
                `SessionStatus` enum. `endedAt`, `durationSeconds` and `amountConsumed` are `null` while
                the session is still `ACTIVE`.
                """
)
public record UsageSessionResponse(
        @Schema(
                description = "Unique identifier of the session.",
                example = "3a1b2c3d-4e5f-6789-abcd-ef0123456789"
        )
        UUID id,
        @Schema(
                description = "Identifier of the user the session belongs to.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID userId,
        @Schema(
                description = "Client-provided reference identifying the session.",
                example = "desktop-client"
        )
        String clientReference,
        @Schema(
                description = "Timestamp (ISO 8601) at which the session was started.",
                example = "2026-08-02T10:15:30"
        )
        LocalDateTime startedAt,
        @Schema(
                description = "Timestamp (ISO 8601) at which the session was ended. Null while active.",
                example = "2026-08-02T11:15:30"
        )
        LocalDateTime endedAt,
        @Schema(
                description = "Duration of the session in seconds. Null while active.",
                example = "3600"
        )
        Long durationSeconds,
        @Schema(
                description = "Total resource units consumed during the session. Null while active.",
                example = "120"
        )
        Integer amountConsumed,
        @Schema(example = "ACTIVE")
        SessionStatus status,
        @Schema(
                description = "Free-form metadata attached to the session.",
                example = "started from desktop client"
        )
        String metadata
) {
}
