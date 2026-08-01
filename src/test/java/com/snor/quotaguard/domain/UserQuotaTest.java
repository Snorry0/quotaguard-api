package com.snor.quotaguard.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserQuotaTest {

    private static UserQuota quotaWith(int dailyLimit, int usedToday, int penaltyLevel) {
        return UserQuota.builder()
                .dailyLimit(dailyLimit)
                .usedToday(usedToday)
                .lastResetDate(LocalDate.of(2026, 8, 1))
                .penaltyLevel(penaltyLevel)
                .build();
    }

    @Test
    void remainingTodayNeverBecomesNegative() {
        assertThat(quotaWith(100, 150, 0).remainingToday()).isZero();
        assertThat(quotaWith(100, 35, 0).remainingToday()).isEqualTo(65);
    }

    @Test
    void canConsumeReflectsAvailableCapacity() {
        UserQuota quota = quotaWith(100, 90, 0);
        assertThat(quota.canConsume(10)).isTrue();
        assertThat(quota.canConsume(11)).isFalse();
    }

    @Test
    void consumeIncreasesUsedToday() {
        UserQuota quota = quotaWith(100, 10, 0);
        quota.consume(15);
        assertThat(quota.getUsedToday()).isEqualTo(25);
    }

    @Test
    void consumeRejectsAmountsBeyondTheLimit() {
        UserQuota quota = quotaWith(100, 95, 0);
        assertThatThrownBy(() -> quota.consume(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resetForNewDayClearsUsageAndDecaysPenalty() {
        UserQuota quota = quotaWith(100, 80, 3);
        quota.resetForNewDay(LocalDate.of(2026, 8, 2), 1);
        assertThat(quota.getUsedToday()).isZero();
        assertThat(quota.getLastResetDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(quota.getPenaltyLevel()).isEqualTo(2);
    }

    @Test
    void resetForNewDayNeverDecaysBelowZero() {
        UserQuota quota = quotaWith(100, 0, 1);
        quota.resetForNewDay(LocalDate.of(2026, 8, 2), 1);
        assertThat(quota.getPenaltyLevel()).isZero();
    }
}
