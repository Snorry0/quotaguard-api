package com.snor.quotaguard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EndUsageSessionRequest(
        @Schema(
                description = "Optional explicit amount to consume. If omitted, backend calculates it from session duration.",
                example = "15",
                minimum = "1",
                nullable = true
        )
        @Positive
        Integer amountConsumed,

        @Size(max = 5000)
        String metadata
) {
}