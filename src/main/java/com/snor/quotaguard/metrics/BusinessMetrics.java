package com.snor.quotaguard.metrics;

import com.snor.quotaguard.domain.enums.ActionType;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.domain.enums.SessionStatus;
import com.snor.quotaguard.session.repository.UsageSessionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for QuotaGuard business metrics.
 *
 * <p>All meter names, types and tags are defined here. Counters are incremented
 * through intent-named {@code recordXxx} methods invoked by the metrics listener
 * and (for failed session completions) by the session service. The active-sessions
 * gauge is backed by the repository and polled on every scrape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private static final String TYPE_TAG = "type";

    private final MeterRegistry meterRegistry;
    private final UsageSessionRepository usageSessionRepository;

    private Counter successfulRegistrations;
    private Counter failedRegistrations;
    private Counter successfulLogins;
    private Counter failedLogins;
    private Map<ActionType, Counter> quotaConsumptions;
    private Map<String, Counter> quotaResets;
    private Map<PenaltyType, Counter> penaltiesApplied;
    private Counter penaltyExpirations;
    private Counter completedSessions;
    private Counter failedSessionCompletions;
    private Map<String, Counter> adminOperations;

    @PostConstruct
    void init() {
        successfulRegistrations = counter("quotaguard.registrations.successful", "Successful user registrations");
        failedRegistrations = counter("quotaguard.registrations.failed", "Failed user registrations");
        successfulLogins = counter("quotaguard.logins.successful", "Successful logins");
        failedLogins = counter("quotaguard.logins.failed", "Failed logins");

        quotaConsumptions = new EnumMap<>(ActionType.class);
        for (ActionType actionType : ActionType.values()) {
            quotaConsumptions.put(actionType, taggedCounter(
                    "quotaguard.quota.consumptions", "actionType", actionType.name(),
                    "Quota consumption events"));
        }

        quotaResets = new HashMap<>();
        quotaResets.put("daily", taggedCounter("quotaguard.quota.resets", TYPE_TAG, "daily", "Quota resets"));
        quotaResets.put("bulk", taggedCounter("quotaguard.quota.resets", TYPE_TAG, "bulk", "Quota resets"));

        penaltiesApplied = new EnumMap<>(PenaltyType.class);
        for (PenaltyType penaltyType : PenaltyType.values()) {
            penaltiesApplied.put(penaltyType, taggedCounter(
                    "quotaguard.penalties.applied", TYPE_TAG, penaltyType.name(), "Penalties applied"));
        }
        penaltyExpirations = counter("quotaguard.penalties.expired", "Penalties expired");

        completedSessions = counter("quotaguard.sessions.completed", "Usage sessions completed");
        failedSessionCompletions = counter(
                "quotaguard.sessions.completion.failed", "Failed usage session completions");

        adminOperations = new HashMap<>();
        adminOperations.put("user_create", taggedCounter(
                "quotaguard.admin.operations", TYPE_TAG, "user_create", "Manual admin operations"));
        adminOperations.put("user_update", taggedCounter(
                "quotaguard.admin.operations", TYPE_TAG, "user_update", "Manual admin operations"));
        adminOperations.put("user_delete", taggedCounter(
                "quotaguard.admin.operations", TYPE_TAG, "user_delete", "Manual admin operations"));
        adminOperations.put("quota_reset", taggedCounter(
                "quotaguard.admin.operations", TYPE_TAG, "quota_reset", "Manual admin operations"));

        // Gauge state: use `this` (a Spring singleton, always strongly reachable) so Micrometer's
        // weak reference to the state object doesn't get GC'd. The ToDoubleFunction reads the
        // current value from the repository on every scrape (the `quotaguard.sessions.active` meter
        // is per-scrape, not cached — cheap indexed query).
        Gauge.builder("quotaguard.sessions.active", this, m -> m.countActiveSessions())
                .description("Currently active usage sessions")
                .register(meterRegistry);
    }

    double countActiveSessions() {
        return usageSessionRepository.countByStatus(SessionStatus.ACTIVE);
    }

    public void recordSuccessfulRegistration() {
        successfulRegistrations.increment();
    }

    public void recordFailedRegistration() {
        failedRegistrations.increment();
    }

    public void recordSuccessfulLogin() {
        successfulLogins.increment();
    }

    public void recordFailedLogin() {
        failedLogins.increment();
    }

    public void recordQuotaConsumption(ActionType actionType) {
        quotaConsumptions.get(actionType).increment();
    }

    public void recordQuotaReset(String type) {
        Counter counter = quotaResets.get(type);
        if (counter == null) {
            log.warn("No counter registered for quota reset type '{}' — skipping increment", type);
            return;
        }
        counter.increment();
    }

    public void recordPenaltyApplied(PenaltyType type) {
        Counter counter = penaltiesApplied.get(type);
        if (counter == null) {
            log.warn("No counter registered for penalty type '{}' — skipping increment", type);
            return;
        }
        counter.increment();
    }

    public void recordPenaltyExpired() {
        penaltyExpirations.increment();
    }

    public void recordCompletedSession() {
        completedSessions.increment();
    }

    public void recordFailedSessionCompletion() {
        failedSessionCompletions.increment();
    }

    public void recordAdminOperation(String type) {
        Counter counter = adminOperations.get(type);
        if (counter == null) {
            log.warn("No counter registered for admin operation type '{}' — skipping increment", type);
            return;
        }
        counter.increment();
    }

    private Counter counter(String name, String description) {
        return Counter.builder(name).description(description).register(meterRegistry);
    }

    private Counter taggedCounter(String name, String tagKey, String tagValue, String description) {
        return Counter.builder(name).tag(tagKey, tagValue).description(description).register(meterRegistry);
    }
}
