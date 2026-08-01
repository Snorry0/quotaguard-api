package com.snor.quotaguard.session.dto.response;

import com.snor.quotaguard.usage.dto.response.ConsumeUsageResponse;

public record EndUsageSessionResponse(
        UsageSessionResponse session,
        ConsumeUsageResponse consumption
) {
}