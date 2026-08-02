package com.snor.quotaguard.audit;

import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.security.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditPublisherTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);
    private final AuditPublisher auditPublisher = new AuditPublisher(eventPublisher, currentUserProvider, clock);

    @AfterEach
    void cleanUpRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publishForCurrentUserResolvesActorAndRequestIp() {
        User actor = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        when(currentUserProvider.getCurrentUserIfPresent()).thenReturn(Optional.of(actor));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(remoteAddrRequest("10.0.0.7")));

        auditPublisher.publishForCurrentUser(AuditAction.QUOTA_RESET, "QUOTA", null, "reset", true);

        AuditCommand command = capturedCommand();
        assertThat(command.actorId()).isEqualTo(actor.getId());
        assertThat(command.actorEmail()).isEqualTo("admin@example.com");
        assertThat(command.ipAddress()).isEqualTo("10.0.0.7");
        assertThat(command.timestamp()).isEqualTo(Instant.parse("2026-08-02T10:00:00Z"));
    }

    @Test
    void publishForCurrentUserWithoutActorUsesNulls() {
        when(currentUserProvider.getCurrentUserIfPresent()).thenReturn(Optional.empty());

        auditPublisher.publishForCurrentUser(AuditAction.QUOTA_RESET, "QUOTA", null, "scheduled reset", true);

        AuditCommand command = capturedCommand();
        assertThat(command.actorId()).isNull();
        assertThat(command.actorEmail()).isNull();
        assertThat(command.ipAddress()).isNull();
    }

    @Test
    void publishWithActorUsesProvidedActor() {
        UUID actorId = UUID.randomUUID();

        auditPublisher.publishWithActor(
                AuditAction.LOGIN_FAILED, "AUTH", null, "login failed", false, actorId, "attempted@example.com"
        );

        AuditCommand command = capturedCommand();
        assertThat(command.actorId()).isEqualTo(actorId);
        assertThat(command.actorEmail()).isEqualTo("attempted@example.com");
        assertThat(command.success()).isFalse();
        assertThat(command.ipAddress()).isNull();
    }

    private AuditCommand capturedCommand() {
        ArgumentCaptor<AuditCommand> captor = ArgumentCaptor.forClass(AuditCommand.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private MockHttpServletRequest remoteAddrRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
