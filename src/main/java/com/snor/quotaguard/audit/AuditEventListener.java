package com.snor.quotaguard.audit;

import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.audit.service.AuditEventWriter;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.BulkQuotaResetEvent;
import com.snor.quotaguard.event.LoginFailedEvent;
import com.snor.quotaguard.event.LoginSucceededEvent;
import com.snor.quotaguard.event.PenaltyAppliedEvent;
import com.snor.quotaguard.event.PenaltyExpiredEvent;
import com.snor.quotaguard.event.QuotaResetEvent;
import com.snor.quotaguard.event.RegisterFailedEvent;
import com.snor.quotaguard.event.SessionCompletedEvent;
import com.snor.quotaguard.event.SessionStartedEvent;
import com.snor.quotaguard.event.UserCreatedEvent;
import com.snor.quotaguard.event.UserDeletedEvent;
import com.snor.quotaguard.event.UserRegisteredEvent;
import com.snor.quotaguard.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final String RESOURCE_TYPE_AUTH = "AUTH";

    private final AuditEventWriter auditEventWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.REGISTER_SUCCESS,
                RESOURCE_TYPE_AUTH,
                event.userId(),
                "User registered",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserCreated(UserCreatedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.USER_CREATED,
                "USER",
                event.userId(),
                "Admin created user",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserUpdated(UserUpdatedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.USER_UPDATED,
                "USER",
                event.userId(),
                "Admin updated user: " + String.join(", ", event.changedFields()),
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserDeleted(UserDeletedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.USER_DELETED,
                "USER",
                event.userId(),
                "Admin deleted user",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLoginSucceeded(LoginSucceededEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.LOGIN_SUCCESS,
                RESOURCE_TYPE_AUTH,
                event.userId(),
                "User logged in",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onLoginFailed(LoginFailedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.LOGIN_FAILED,
                RESOURCE_TYPE_AUTH,
                null,
                "Login failed: invalid credentials",
                new Actor(null, event.attemptedEmail()),
                false
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onRegisterFailed(RegisterFailedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.REGISTER_FAILED,
                RESOURCE_TYPE_AUTH,
                null,
                "Registration failed",
                new Actor(null, event.attemptedEmail()),
                false
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onQuotaReset(QuotaResetEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.QUOTA_RESET,
                "QUOTA",
                event.quotaId(),
                "Quota reset for new day",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBulkQuotaReset(BulkQuotaResetEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.QUOTA_RESET,
                "QUOTA",
                null,
                "Quotas reset: " + event.resetCount() + " quotas, " + event.expiredPenalties() + " penalties expired",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPenaltyApplied(PenaltyAppliedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.PENALTY_APPLIED,
                "PENALTY",
                event.penaltyId(),
                "Penalty applied: " + event.type(),
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPenaltyExpired(PenaltyExpiredEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.PENALTY_EXPIRED,
                "PENALTY",
                event.penaltyId(),
                "Penalty expired",
                Actor.SYSTEM,
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionStarted(SessionStartedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.SESSION_STARTED,
                "SESSION",
                event.sessionId(),
                "Usage session started",
                event.actor(),
                true
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionCompleted(SessionCompletedEvent event) {
        persistSafely(
                event.timestamp(),
                AuditAction.SESSION_COMPLETED,
                "SESSION",
                event.sessionId(),
                "Usage session completed",
                event.actor(),
                true
        );
    }

    private void persistSafely(
            Instant timestamp,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String description,
            Actor actor,
            boolean success
    ) {
        try {
            auditEventWriter.persist(new AuditCommand(
                    timestamp,
                    action,
                    actor.id(),
                    actor.email(),
                    resourceType,
                    resourceId,
                    description,
                    resolveIpAddress(),
                    success
            ));
        } catch (Exception ex) {
            log.error("Failed to persist audit event for action {}: {}", action, ex.getMessage());
        }
    }

    private String resolveIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
