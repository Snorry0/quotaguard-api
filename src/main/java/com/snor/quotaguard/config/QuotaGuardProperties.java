package com.snor.quotaguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "quotaguard")
public record QuotaGuardProperties(
        int defaultDailyLimit,
        int penaltyDecayPerReset,
        String resetCron,
        Penalties penalties,
        Sessions sessions
) {
    public QuotaGuardProperties {
        if (defaultDailyLimit <= 0) {
            defaultDailyLimit = 100;
        }
        if (penaltyDecayPerReset < 0) {
            penaltyDecayPerReset = 0;
        }
        if (penalties == null) {
            penalties = new Penalties(Duration.ofMinutes(15), Duration.ofHours(4));
        }
        if (sessions == null) {
            sessions = new Sessions(1, 1);
        }
    }

    public record Penalties(
            Duration shortCooldown,
            Duration longCooldown
    ) {
        public Penalties {
            if (shortCooldown == null) {
                shortCooldown = Duration.ofMinutes(15);
            }
            if (longCooldown == null) {
                longCooldown = Duration.ofHours(4);
            }
        }
    }

    public record Sessions(
            int unitsPerMinute,
            int minimumCharge
    ) {
        public Sessions {
            if (unitsPerMinute <= 0) {
                unitsPerMinute = 1;
            }
            if (minimumCharge < 0) {
                minimumCharge = 0;
            }
        }
    }
}