package com.snor.quotaguard.quota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Result of a global quota reset.
 */
@Schema(
        description = """
                Result of a global quota reset (ADMIN only): the number of quotas reset, the date the
                new quota period starts and how many active penalties expired as part of the reset.
                """
)
public record QuotaResetResponse(
        @Schema(
                description = "Number of quota records reset.",
                example = "42"
        )
        int resetCount,
        @Schema(
                description = "Date of the new quota period.",
                example = "2026-08-02"
        )
        LocalDate resetDate,
        @Schema(
                description = "Number of penalties that expired as part of the reset.",
                example = "3"
        )
        int expiredPenalties
) {
}
