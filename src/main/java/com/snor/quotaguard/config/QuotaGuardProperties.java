package com.snor.quotaguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "quotaguard")
public record QuotaGuardProperties(
        int defaultDailyLimit,
        int penaltyDecayPerReset,
        Penalties penalties,
        Sessions sessions
) {
    public record Penalties(
            Duration shortCooldown,
            Duration longCooldown
    ) {
    }

    public record Sessions(
            int unitsPerMinute,
            int minimumCharge
    ) {
    }
}
