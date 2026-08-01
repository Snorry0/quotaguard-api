package com.snor.quotaguard.analytics.dto.response;

public record BehaviorInsightResponse(
        String code,
        InsightSeverity severity,
        String message
) {
}
