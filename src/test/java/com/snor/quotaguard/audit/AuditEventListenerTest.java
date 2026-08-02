package com.snor.quotaguard.audit;

import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.audit.service.AuditEventWriter;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.BulkQuotaResetEvent;
import com.snor.quotaguard.event.LoginFailedEvent;
import com.snor.quotaguard.event.PenaltyAppliedEvent;
import com.snor.quotaguard.event.PenaltyExpiredEvent;
import com.snor.quotaguard.event.QuotaResetEvent;
import com.snor.quotaguard.event.SessionStartedEvent;
import com.snor.quotaguard.event.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventListenerTest {

    private final AuditEventWriter auditEventWriter = mock(AuditEventWriter.class);
    private final AuditEventListener listener = new AuditEventListener(auditEventWriter);

    @Test
    void userCreatedEventProducesUserCreatedAuditCommand() {
        UUID userId = UUID.randomUUID();
        Actor admin = new Actor(UUID.randomUUID(), "admin@example.com");

        listener.onUserCreated(new UserCreatedEvent(
                Instant.parse("2026-08-02T10:00:00Z"), admin, userId, "user@example.com", "USER"
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.USER_CREATED);
        assertThat(command.resourceType()).isEqualTo("USER");
        assertThat(command.resourceId()).isEqualTo(userId);
        assertThat(command.actorId()).isEqualTo(admin.id());
        assertThat(command.actorEmail()).isEqualTo("admin@example.com");
        assertThat(command.success()).isTrue();
    }

    @Test
    void loginFailedEventProducesFailedAuditCommandWithAttemptedEmail() {
        listener.onLoginFailed(new LoginFailedEvent(
                Instant.parse("2026-08-02T10:00:00Z"), "attempted@example.com"
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.LOGIN_FAILED);
        assertThat(command.actorId()).isNull();
        assertThat(command.actorEmail()).isEqualTo("attempted@example.com");
        assertThat(command.success()).isFalse();
    }

    @Test
    void penaltyExpiredEventHasNoActor() {
        UUID penaltyId = UUID.randomUUID();

        listener.onPenaltyExpired(new PenaltyExpiredEvent(
                Instant.parse("2026-08-02T10:00:00Z"), penaltyId
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.PENALTY_EXPIRED);
        assertThat(command.resourceId()).isEqualTo(penaltyId);
        assertThat(command.actorId()).isNull();
        assertThat(command.actorEmail()).isNull();
    }

    @Test
    void bulkQuotaResetProducesCountsDescription() {
        listener.onBulkQuotaReset(new BulkQuotaResetEvent(
                Instant.parse("2026-08-02T10:00:00Z"), Actor.SYSTEM, 12, 3
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.QUOTA_RESET);
        assertThat(command.resourceId()).isNull();
        assertThat(command.description()).contains("12 quotas", "3 penalties");
    }

    @Test
    void lazyQuotaResetProducesDailyDescription() {
        UUID quotaId = UUID.randomUUID();

        listener.onQuotaReset(new QuotaResetEvent(
                Instant.parse("2026-08-02T10:00:00Z"), Actor.SYSTEM, quotaId
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.QUOTA_RESET);
        assertThat(command.resourceId()).isEqualTo(quotaId);
        assertThat(command.description()).isEqualTo("Quota reset for new day");
    }

    @Test
    void sessionEventCarriesSessionResource() {
        UUID sessionId = UUID.randomUUID();
        Actor actor = new Actor(UUID.randomUUID(), "user@example.com");

        listener.onSessionStarted(new SessionStartedEvent(
                Instant.parse("2026-08-02T10:00:00Z"), actor, sessionId
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.SESSION_STARTED);
        assertThat(command.resourceType()).isEqualTo("SESSION");
        assertThat(command.resourceId()).isEqualTo(sessionId);
    }

    @Test
    void penaltyAppliedEventCarriesType() {
        UUID penaltyId = UUID.randomUUID();
        Actor actor = new Actor(UUID.randomUUID(), "user@example.com");

        listener.onPenaltyApplied(new PenaltyAppliedEvent(
                Instant.parse("2026-08-02T10:00:00Z"), actor, penaltyId, PenaltyType.SHORT_COOLDOWN
        ));

        AuditCommand command = capturedCommand();
        assertThat(command.action()).isEqualTo(AuditAction.PENALTY_APPLIED);
        assertThat(command.description()).contains("SHORT_COOLDOWN");
    }

    @Test
    void listenerSwallowsPersistenceFailure() {
        doThrow(new RuntimeException("database is down"))
                .when(auditEventWriter).persist(any(AuditCommand.class));

        assertThatCode(() -> listener.onUserCreated(new UserCreatedEvent(
                Instant.parse("2026-08-02T10:00:00Z"),
                Actor.SYSTEM,
                UUID.randomUUID(),
                "user@example.com",
                "USER"
        ))).doesNotThrowAnyException();
    }

    private AuditCommand capturedCommand() {
        ArgumentCaptor<AuditCommand> captor = ArgumentCaptor.forClass(AuditCommand.class);
        verify(auditEventWriter).persist(captor.capture());
        return captor.getValue();
    }
}
