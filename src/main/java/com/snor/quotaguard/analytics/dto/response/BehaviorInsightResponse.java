package com.snor.quotaguard.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A behavioural insight computed from usage statistics.
 */
@Schema(
        description = """
                A behavioural insight computed from usage statistics. `severity` is one of the values
                documented on the `InsightSeverity` enum.
                """
)
public record BehaviorInsightResponse(
        @Schema(
                description = "Stable machine-readable code identifying the insight.",
                example = "HIGH_LIMIT_UTILIZATION"
        )
        String code,
        @Schema(example = "WARN")
        InsightSeverity severity,
        @Schema(
                description = "Human-readable description of the insight.",
                example = "Average daily usage is close to the configured daily limit."
        )
        String message
) {
}
