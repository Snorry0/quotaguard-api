package com.snor.quotaguard.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EndUsageSessionRequest(
        @Positive
        Integer amountConsumed,

        @Size(max = 5000)
        String metadata
) {
}