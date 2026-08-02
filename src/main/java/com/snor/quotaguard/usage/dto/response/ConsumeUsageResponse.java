package com.snor.quotaguard.usage.dto.response;

import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of consuming resource units.
 */
@Schema(
        description = """
                Result of consuming resource units: the recorded usage entry and the quota state after
                the consumption was applied.
                """
)
public record ConsumeUsageResponse(
        @Schema(
                description = "The recorded usage entry."
        )
        UsageRecordResponse usage,
        @Schema(
                description = "The caller's quota after the consumption was applied."
        )
        QuotaResponse quota
) {
}
