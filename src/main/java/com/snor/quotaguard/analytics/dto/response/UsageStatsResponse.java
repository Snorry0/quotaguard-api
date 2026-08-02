package com.snor.quotaguard.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Usage statistics for the selected period.
 */
@Schema(
        description = """
                Usage statistics for the selected period, including totals, averages, over-limit
                attempts and behavioural insights. `from` and `to` are inclusive bounds of the
                reported period.
                """
)
public record UsageStatsResponse(
        @Schema(
                description = "Start date of the reported period (inclusive).",
                example = "2026-07-27"
        )
        LocalDate from,
        @Schema(
                description = "End date of the reported period (inclusive).",
                example = "2026-08-02"
        )
        LocalDate to,
        @Schema(
                description = "Total resource units consumed in the period.",
                example = "1500"
        )
        long totalConsumed,
        @Schema(
                description = "Number of recorded usage events in the period.",
                example = "180"
        )
        long eventCount,
        @Schema(
                description = "Average units consumed per recorded event.",
                example = "8.33"
        )
        double averageUsagePerEvent,
        @Schema(
                description = "Average units consumed per day in the period.",
                example = "214.29"
        )
        double averageDailyUsage,
        @Schema(
                description = "Number of attempts rejected because they exceeded the daily quota.",
                example = "12"
        )
        long overLimitEvents,
        @Schema(
                description = "Share of attempts rejected for exceeding the quota (0.0 to 1.0).",
                example = "0.06"
        )
        double overLimitFrequency,
        @Schema(
                description = "Behavioural insights derived from the period's usage pattern."
        )
        List<BehaviorInsightResponse> insights
) {
}
