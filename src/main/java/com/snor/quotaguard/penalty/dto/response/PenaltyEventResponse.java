package com.snor.quotaguard.penalty.dto.response;

import com.snor.quotaguard.domain.enums.PenaltyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A penalty event applied to a user.
 */
@Schema(
        description = """
                A penalty event applied to a user. `type` is one of the values documented on the
                `PenaltyType` enum. `active` is `true` while the penalty is blocking or advisory.
                """
)
public record PenaltyEventResponse(
        @Schema(
                description = "Unique identifier of the penalty event.",
                example = "7f8091a2-3d4e-5f6a-9b0c-1234567890de"
        )
        UUID id,
        @Schema(
                description = "Identifier of the user the penalty was applied to.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID userId,
        @Schema(example = "SHORT_COOLDOWN")
        PenaltyType type,
        @Schema(
                description = "Timestamp (ISO 8601) at which the penalty was applied.",
                example = "2026-08-02T12:00:00"
        )
        LocalDateTime startTime,
        @Schema(
                description = "Timestamp (ISO 8601) at which the penalty ends. Equals `startTime` for `WARNING` penalties.",
                example = "2026-08-02T12:15:00"
        )
        LocalDateTime endTime,
        @Schema(
                description = "Whether the penalty is currently active.",
                example = "true"
        )
        boolean active
) {
}
