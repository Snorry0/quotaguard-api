package com.snor.quotaguard.dto.response;

import java.time.LocalDate;

public record UsageTrendResponse(
        LocalDate date,
        long totalConsumed,
        long eventCount
) {
}
