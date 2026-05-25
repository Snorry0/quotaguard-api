package com.snor.quotaguard.dto.response;

public record BehaviorInsightResponse(
        String code,
        String severity,
        String message
) {
}
