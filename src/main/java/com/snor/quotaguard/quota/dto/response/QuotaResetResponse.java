package com.snor.quotaguard.quota.dto.response;

import java.time.LocalDate;

public record QuotaResetResponse(
        int resetCount,
        LocalDate resetDate,
        int expiredPenalties
) {
}
