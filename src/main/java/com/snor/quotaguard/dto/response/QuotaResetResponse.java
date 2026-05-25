package com.snor.quotaguard.dto.response;

import java.time.LocalDate;

public record QuotaResetResponse(
        int resetCount,
        LocalDate resetDate
) {
}
