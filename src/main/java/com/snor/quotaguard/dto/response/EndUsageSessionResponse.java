package com.snor.quotaguard.dto.response;

public record EndUsageSessionResponse(
        UsageSessionResponse session,
        ConsumeUsageResponse consumption
) {
}