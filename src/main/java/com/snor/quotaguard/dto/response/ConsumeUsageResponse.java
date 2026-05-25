package com.snor.quotaguard.dto.response;

public record ConsumeUsageResponse(
        UsageRecordResponse usage,
        QuotaResponse quota
) {
}
