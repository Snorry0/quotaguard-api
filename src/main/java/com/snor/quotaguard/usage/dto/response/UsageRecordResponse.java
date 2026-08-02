package com.snor.quotaguard.usage.dto.response;

import com.snor.quotaguard.domain.enums.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single recorded usage consumption entry.
 */
@Schema(
        description = """
                A single recorded usage consumption entry. `actionType` is one of the values documented
                on the `ActionType` enum.
                """
)
public record UsageRecordResponse(
        @Schema(
                description = "Unique identifier of the usage record.",
                example = "4d5e6f70-1a2b-3c4d-8e9f-0123456789ab"
        )
        UUID id,
        @Schema(
                description = "Identifier of the user the usage is recorded for.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID userId,
        @Schema(
                description = "Amount of resource units consumed.",
                example = "10"
        )
        int amountConsumed,
        @Schema(example = "API_CALL")
        ActionType actionType,
        @Schema(
                description = "Timestamp (ISO 8601) at which the usage was recorded.",
                example = "2026-08-02T10:15:30"
        )
        LocalDateTime timestamp
) {
}
