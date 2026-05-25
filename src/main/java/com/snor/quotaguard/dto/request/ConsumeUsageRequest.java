package com.snor.quotaguard.dto.request;

import com.snor.quotaguard.domain.enums.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumeUsageRequest(
        @Schema(
                description = "Amount of resource units consumed by this action.",
                example = "10",
                minimum = "1"
        )
        @NotNull
        @Positive
        Integer amountConsumed,

        @Schema(
                description = "Type of action that caused the usage consumption.",
                example = "API_CALL",
                allowableValues = {
                        "API_CALL",
                        "RESOURCE_ACCESS",
                        "BACKGROUND_JOB",
                        "SESSION_ACTION",
                        "MANUAL_ADJUSTMENT"
                }
        )
        @NotNull
        ActionType actionType
) {
}
