package com.snor.quotaguard.usage.dto.request;

import com.snor.quotaguard.domain.enums.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for consuming resource units.
 */
@Schema(
        description = """
                Payload for consuming resource units against the caller's quota. The consumption is
                recorded and the updated quota is returned. See `ActionType` for the possible action
                types and their meanings.
                """
)
public record ConsumeUsageRequest(
        @Schema(
                description = "Amount of resource units consumed by this action.",
                example = "10",
                minimum = "1"
        )
        @NotNull
        @Positive
        Integer amountConsumed,

        @Schema(example = "API_CALL")
        @NotNull
        ActionType actionType
) {
}
