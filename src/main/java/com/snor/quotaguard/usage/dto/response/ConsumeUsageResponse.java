package com.snor.quotaguard.usage.dto.response;

import com.snor.quotaguard.quota.dto.response.QuotaResponse;

public record ConsumeUsageResponse(
        UsageRecordResponse usage,
        QuotaResponse quota
) {
}
