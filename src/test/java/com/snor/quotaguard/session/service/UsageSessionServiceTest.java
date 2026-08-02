package com.snor.quotaguard.session.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.UsageSession;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.SessionStatus;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.SessionCompletedEvent;
import com.snor.quotaguard.event.SessionStartedEvent;
import com.snor.quotaguard.session.dto.request.EndUsageSessionRequest;
import com.snor.quotaguard.session.dto.request.StartUsageSessionRequest;
import com.snor.quotaguard.session.mapper.UsageSessionMapper;
import com.snor.quotaguard.session.repository.UsageSessionRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import com.snor.quotaguard.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageSessionServiceTest {

    private final UsageSessionRepository usageSessionRepository = mock(UsageSessionRepository.class);
    private final UsageSessionMapper usageSessionMapper = mock(UsageSessionMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final UsageService usageService = mock(UsageService.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);
    private final QuotaGuardProperties properties = new QuotaGuardProperties(
            100,
            1,
            new QuotaGuardProperties.Penalties(Duration.ofMinutes(15), Duration.ofHours(4)),
            new QuotaGuardProperties.Sessions(1, 1)
    );

    private final UsageSessionService usageSessionService = new UsageSessionService(
            usageSessionRepository,
            usageSessionMapper,
            currentUserProvider,
            usageService,
            domainEventPublisher,
            properties,
            clock
    );

    @Test
    void startSessionPublishesSessionStartedEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(usageSessionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
                user, SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        UUID sessionId = UUID.randomUUID();
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(invocation -> {
            UsageSession session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });

        usageSessionService.startSession(new StartUsageSessionRequest("client", null));

        ArgumentCaptor<SessionStartedEvent> captor = ArgumentCaptor.forClass(SessionStartedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().actor()).isEqualTo(new Actor(user.getId(), user.getEmail()));
    }

    @Test
    void endSessionPublishesSessionCompletedEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        UsageSession session = UsageSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .startedAt(java.time.LocalDateTime.now(clock))
                .status(SessionStatus.ACTIVE)
                .build();
        when(usageSessionRepository.findByIdAndUserIdForUpdate(session.getId(), user.getId()))
                .thenReturn(Optional.of(session));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usageSessionService.endSession(session.getId(), new EndUsageSessionRequest(5, null));

        ArgumentCaptor<SessionCompletedEvent> captor = ArgumentCaptor.forClass(SessionCompletedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo(session.getId());
        assertThat(captor.getValue().actor()).isEqualTo(new Actor(user.getId(), user.getEmail()));
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }
}
