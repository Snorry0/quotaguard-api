package com.snor.quotaguard.service;

import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.dto.response.BehaviorInsightResponse;
import com.snor.quotaguard.dto.response.UsageStatsResponse;
import com.snor.quotaguard.dto.response.UsageTrendResponse;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.repository.PenaltyEventRepository;
import com.snor.quotaguard.repository.UsageRecordRepository;
import com.snor.quotaguard.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int MAX_RANGE_DAYS = 366;

    private final CurrentUserProvider currentUserProvider;
    private final UsageRecordRepository usageRecordRepository;
    private final PenaltyEventRepository penaltyEventRepository;
    private final UserQuotaRepository userQuotaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UsageStatsResponse getUsageStats(int days) {
        int safeDays = sanitizeDays(days);
        User user = currentUserProvider.getCurrentUser();
        UserQuota quota = userQuotaRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Quota not found for current user"));

        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(safeDays - 1L);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<UsageRecord> records = usageRecordRepository.findByUserAndTimestampBetweenOrderByTimestampAsc(user, start, end);
        long totalConsumed = records.stream().mapToLong(UsageRecord::getAmountConsumed).sum();
        long eventCount = records.size();
        double averageUsagePerEvent = eventCount == 0 ? 0.0 : (double) totalConsumed / eventCount;
        double averageDailyUsage = (double) totalConsumed / safeDays;
        long overLimitEvents = countOverLimitEvents(user, start, end);
        long attemptCount = eventCount + overLimitEvents;
        double overLimitFrequency = attemptCount == 0 ? 0.0 : (double) overLimitEvents / attemptCount;

        List<UsageTrendResponse> trend = buildTrend(records, from, to);
        List<BehaviorInsightResponse> insights = buildInsights(
                quota,
                trend,
                totalConsumed,
                eventCount,
                averageDailyUsage,
                overLimitFrequency
        );

        return new UsageStatsResponse(
                from,
                to,
                totalConsumed,
                eventCount,
                round(averageUsagePerEvent),
                round(averageDailyUsage),
                overLimitEvents,
                round(overLimitFrequency),
                insights
        );
    }

    @Transactional(readOnly = true)
    public List<UsageTrendResponse> getUsageTrend(int days) {
        int safeDays = sanitizeDays(days);
        User user = currentUserProvider.getCurrentUser();
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(safeDays - 1L);
        List<UsageRecord> records = usageRecordRepository.findByUserAndTimestampBetweenOrderByTimestampAsc(
                user,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );
        return buildTrend(records, from, to);
    }

    private long countOverLimitEvents(User user, LocalDateTime start, LocalDateTime end) {
        return penaltyEventRepository.countByUserAndTypeInAndStartTimeBetween(
                user,
                EnumSet.allOf(PenaltyType.class),
                start,
                end
        );
    }

    private List<UsageTrendResponse> buildTrend(List<UsageRecord> records, LocalDate from, LocalDate to) {
        Map<LocalDate, List<UsageRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(record -> record.getTimestamp().toLocalDate()));

        List<UsageTrendResponse> trend = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            List<UsageRecord> dayRecords = byDate.getOrDefault(cursor, List.of());
            long total = dayRecords.stream().mapToLong(UsageRecord::getAmountConsumed).sum();
            trend.add(new UsageTrendResponse(cursor, total, dayRecords.size()));
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private List<BehaviorInsightResponse> buildInsights(
            UserQuota quota,
            List<UsageTrendResponse> trend,
            long totalConsumed,
            long eventCount,
            double averageDailyUsage,
            double overLimitFrequency
    ) {
        List<BehaviorInsightResponse> insights = new ArrayList<>();

        if (eventCount == 0) {
            insights.add(new BehaviorInsightResponse(
                    "NO_RECENT_ACTIVITY",
                    "INFO",
                    "No usage events were recorded in the selected period."
            ));
            return insights;
        }

        double limitUtilization = quota.getDailyLimit() == 0 ? 0.0 : averageDailyUsage / quota.getDailyLimit();
        if (limitUtilization >= 0.8) {
            insights.add(new BehaviorInsightResponse(
                    "HIGH_LIMIT_UTILIZATION",
                    "WARN",
                    "Average daily usage is close to the configured daily limit."
            ));
        }

        if (overLimitFrequency >= 0.2) {
            insights.add(new BehaviorInsightResponse(
                    "REPEATED_OVER_LIMIT_ATTEMPTS",
                    "RISK",
                    "The user frequently attempts consumption beyond the allowed quota."
            ));
        }

        long maxDailyUsage = trend.stream().mapToLong(UsageTrendResponse::totalConsumed).max().orElse(0);
        if (averageDailyUsage > 0 && maxDailyUsage > averageDailyUsage * 2) {
            insights.add(new BehaviorInsightResponse(
                    "BURSTY_USAGE_PATTERN",
                    "INFO",
                    "Usage is concentrated in spikes instead of being evenly distributed."
            ));
        }

        if (insights.isEmpty()) {
            insights.add(new BehaviorInsightResponse(
                    "STABLE_USAGE_PATTERN",
                    "INFO",
                    "Usage appears stable and within expected limits for the selected period."
            ));
        }
        return insights;
    }

    private int sanitizeDays(int days) {
        if (days <= 0) {
            return 7;
        }
        return Math.min(days, MAX_RANGE_DAYS);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
