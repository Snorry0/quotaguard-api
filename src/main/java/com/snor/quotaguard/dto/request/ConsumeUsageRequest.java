package com.snor.quotaguard.dto.request;

import com.snor.quotaguard.domain.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumeUsageRequest(
        @Positive int amountConsumed,
        @NotNull ActionType actionType
) {
}
