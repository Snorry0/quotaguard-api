package com.snor.quotaguard.dto.response;

import java.time.LocalDate;
import java.util.List;

public record UsageStatsResponse(
        LocalDate from,
        LocalDate to,
        long totalConsumed,
        long eventCount,
        double averageUsagePerEvent,
        double averageDailyUsage,
        long overLimitEvents,
        double overLimitFrequency,
        List<BehaviorInsightResponse> insights
) {
}
