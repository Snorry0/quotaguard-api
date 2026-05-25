package com.snor.quotaguard.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record QuotaResponse(
        UUID id,
        UUID userId,
        int dailyLimit,
        int usedToday,
        int remainingToday,
        LocalDate lastResetDate,
        int penaltyLevel
) {
}
