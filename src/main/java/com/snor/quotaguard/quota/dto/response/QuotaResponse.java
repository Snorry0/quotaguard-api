package com.snor.quotaguard.quota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The caller's current quota state.
 */
@Schema(
        description = """
                The caller's current quota state: the configured daily limit, the usage already applied
                today and the remaining units. `remainingToday` is the difference between `dailyLimit`
                and `usedToday` (never negative).
                """
)
public record QuotaResponse(
        @Schema(
                description = "Unique identifier of the quota record.",
                example = "6e7f8091-2c3d-4e5f-9a0b-1234567890cd"
        )
        UUID id,
        @Schema(
                description = "Identifier of the user the quota belongs to.",
                example = "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8"
        )
        UUID userId,
        @Schema(
                description = "Configured daily limit of resource units.",
                example = "1000"
        )
        int dailyLimit,
        @Schema(
                description = "Resource units consumed today.",
                example = "245"
        )
        int usedToday,
        @Schema(
                description = "Resource units remaining today (`dailyLimit` minus `usedToday`).",
                example = "755"
        )
        int remainingToday,
        @Schema(
                description = "Date of the last quota reset. Aligns with the current quota period.",
                example = "2026-08-02"
        )
        LocalDate lastResetDate,
        @Schema(
                description = "Current penalty level applied to the quota. Higher values indicate more severe penalties.",
                example = "0"
        )
        int penaltyLevel
) {
}
