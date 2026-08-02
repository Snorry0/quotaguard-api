package com.snor.quotaguard.metrics;

import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.BulkQuotaResetEvent;
import com.snor.quotaguard.event.LoginFailedEvent;
import com.snor.quotaguard.event.LoginSucceededEvent;
import com.snor.quotaguard.event.PenaltyAppliedEvent;
import com.snor.quotaguard.event.PenaltyExpiredEvent;
import com.snor.quotaguard.event.QuotaResetEvent;
import com.snor.quotaguard.event.RegisterFailedEvent;
import com.snor.quotaguard.event.SessionCompletedEvent;
import com.snor.quotaguard.event.UserCreatedEvent;
import com.snor.quotaguard.event.UserDeletedEvent;
import com.snor.quotaguard.event.UserRegisteredEvent;
import com.snor.quotaguard.event.UserUpdatedEvent;
import com.snor.quotaguard.event.UsageConsumedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumes domain events and records business metrics. Mirrors the audit listener:
 * one {@link TransactionalEventListener} per event type, each handler wrapped so a
 * metric-recording failure never propagates into the business transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsEventListener {

    private final BusinessMetrics businessMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        recordSafely(businessMetrics::recordSuccessfulRegistration, "recordSuccessfulRegistration");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onRegisterFailed(RegisterFailedEvent event) {
        recordSafely(businessMetrics::recordFailedRegistration, "recordFailedRegistration");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLoginSucceeded(LoginSucceededEvent event) {
        recordSafely(businessMetrics::recordSuccessfulLogin, "recordSuccessfulLogin");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onLoginFailed(LoginFailedEvent event) {
        recordSafely(businessMetrics::recordFailedLogin, "recordFailedLogin");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUsageConsumed(UsageConsumedEvent event) {
        recordSafely(
                () -> businessMetrics.recordQuotaConsumption(event.actionType()),
                "recordQuotaConsumption"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onQuotaReset(QuotaResetEvent event) {
        recordSafely(() -> businessMetrics.recordQuotaReset("daily"), "recordQuotaReset(daily)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBulkQuotaReset(BulkQuotaResetEvent event) {
        recordSafely(() -> businessMetrics.recordQuotaReset("bulk"), "recordQuotaReset(bulk)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBulkQuotaResetAdmin(BulkQuotaResetEvent event) {
        if (event.actor() != null && !Actor.SYSTEM.equals(event.actor())) {
            recordSafely(
                    () -> businessMetrics.recordAdminOperation("quota_reset"),
                    "recordAdminOperation(quota_reset)"
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPenaltyApplied(PenaltyAppliedEvent event) {
        recordSafely(
                () -> businessMetrics.recordPenaltyApplied(event.type()),
                "recordPenaltyApplied"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPenaltyExpired(PenaltyExpiredEvent event) {
        recordSafely(businessMetrics::recordPenaltyExpired, "recordPenaltyExpired");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionCompleted(SessionCompletedEvent event) {
        recordSafely(businessMetrics::recordCompletedSession, "recordCompletedSession");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserCreated(UserCreatedEvent event) {
        recordSafely(() -> businessMetrics.recordAdminOperation("user_create"), "recordAdminOperation(user_create)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserUpdated(UserUpdatedEvent event) {
        recordSafely(() -> businessMetrics.recordAdminOperation("user_update"), "recordAdminOperation(user_update)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserDeleted(UserDeletedEvent event) {
        recordSafely(() -> businessMetrics.recordAdminOperation("user_delete"), "recordAdminOperation(user_delete)");
    }

    private void recordSafely(Runnable recording, String metricName) {
        try {
            recording.run();
        } catch (Exception ex) {
            log.error("Failed to record metric {}: {}", metricName, ex.getMessage());
        }
    }
}
