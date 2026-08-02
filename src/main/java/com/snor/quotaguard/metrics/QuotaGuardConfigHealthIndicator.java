package com.snor.quotaguard.metrics;

import com.snor.quotaguard.config.QuotaGuardProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Validates the quota-reset configuration. Registered automatically as the
 * {@code quotaguardConfig} component of the health endpoint. First failed check
 * short-circuits with {@link Health#down()}.
 */
@Component
@RequiredArgsConstructor
public class QuotaGuardConfigHealthIndicator implements HealthIndicator {

    private final QuotaGuardProperties properties;

    @Value("${quotaguard.reset-cron}")
    private String resetCron;

    @Override
    public Health health() {
        if (properties.defaultDailyLimit() <= 0) {
            return Health.down()
                    .withDetail("defaultDailyLimit", properties.defaultDailyLimit())
                    .withDetail("reason", "must be > 0")
                    .build();
        }

        if (properties.penaltyDecayPerReset() < 0) {
            return Health.down()
                    .withDetail("penaltyDecayPerReset", properties.penaltyDecayPerReset())
                    .withDetail("reason", "must be >= 0")
                    .build();
        }

        QuotaGuardProperties.Penalties penalties = properties.penalties();
        if (!isPositive(penalties.shortCooldown())) {
            return Health.down()
                    .withDetail("shortCooldown", penalties.shortCooldown())
                    .withDetail("reason", "shortCooldown must be > 0")
                    .build();
        }

        if (!isPositive(penalties.longCooldown())) {
            return Health.down()
                    .withDetail("longCooldown", penalties.longCooldown())
                    .withDetail("reason", "longCooldown must be > 0")
                    .build();
        }

        if (penalties.shortCooldown().compareTo(penalties.longCooldown()) >= 0) {
            return Health.down()
                    .withDetail("shortCooldown", penalties.shortCooldown())
                    .withDetail("longCooldown", penalties.longCooldown())
                    .withDetail("reason", "shortCooldown must be < longCooldown")
                    .build();
        }

        if (properties.sessions().unitsPerMinute() <= 0) {
            return Health.down()
                    .withDetail("unitsPerMinute", properties.sessions().unitsPerMinute())
                    .withDetail("reason", "must be > 0")
                    .build();
        }

        if (properties.sessions().minimumCharge() <= 0) {
            return Health.down()
                    .withDetail("minimumCharge", properties.sessions().minimumCharge())
                    .withDetail("reason", "must be > 0")
                    .build();
        }

        try {
            CronExpression.parse(resetCron);
        } catch (IllegalArgumentException ex) {
            return Health.down()
                    .withDetail("resetCron", resetCron)
                    .withDetail("reason", "invalid cron expression")
                    .build();
        }

        return Health.up()
                .withDetail("defaultDailyLimit", properties.defaultDailyLimit())
                .withDetail("resetCron", resetCron)
                .withDetail("checks", "all pass")
                .build();
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && duration.compareTo(Duration.ZERO) > 0;
    }
}
