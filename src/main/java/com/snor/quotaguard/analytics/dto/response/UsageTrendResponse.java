package com.snor.quotaguard.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Daily usage total for a single day in a trend.
 */
@Schema(
        description = """
                Daily usage total for a single day in a trend. Dates with no recorded usage are
                included with a zero total.
                """
)
public record UsageTrendResponse(
        @Schema(
                description = "The day this trend point covers.",
                example = "2026-08-01"
        )
        LocalDate date,
        @Schema(
                description = "Total resource units consumed that day.",
                example = "300"
        )
        long totalConsumed,
        @Schema(
                description = "Number of recorded usage events that day.",
                example = "25"
        )
        long eventCount
) {
}
