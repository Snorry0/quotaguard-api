package com.snor.quotaguard.quota.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.BulkQuotaResetEvent;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.QuotaResetEvent;
import com.snor.quotaguard.penalty.service.PenaltyService;
import com.snor.quotaguard.quota.mapper.UserQuotaMapper;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuotaServiceTest {

    private final UserQuotaRepository userQuotaRepository = mock(UserQuotaRepository.class);
    private final UserQuotaMapper userQuotaMapper = mock(UserQuotaMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final PenaltyService penaltyService = mock(PenaltyService.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);
    private final QuotaGuardProperties properties = new QuotaGuardProperties(
            100,
            1,
            new QuotaGuardProperties.Penalties(Duration.ofMinutes(15), Duration.ofHours(4)),
            new QuotaGuardProperties.Sessions(1, 1)
    );

    private final QuotaService quotaService = new QuotaService(
            userQuotaRepository,
            userQuotaMapper,
            currentUserProvider,
            properties,
            penaltyService,
            domainEventPublisher,
            clock
    );

    @Test
    void lazyResetPublishesQuotaResetEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        UserQuota quota = UserQuota.builder()
                .id(UUID.randomUUID())
                .dailyLimit(100)
                .usedToday(50)
                .lastResetDate(LocalDate.now(clock).minusDays(1))
                .build();

        quotaService.resetIfNewDay(quota);

        ArgumentCaptor<QuotaResetEvent> captor = ArgumentCaptor.forClass(QuotaResetEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().quotaId()).isEqualTo(quota.getId());
        assertThat(captor.getValue().actor()).isEqualTo(Actor.SYSTEM);
        assertThat(quota.getUsedToday()).isZero();
    }

    @Test
    void resetOnCurrentDayDoesNotPublishEvent() {
        UserQuota quota = UserQuota.builder()
                .id(UUID.randomUUID())
                .dailyLimit(100)
                .usedToday(10)
                .lastResetDate(LocalDate.now(clock))
                .build();

        quotaService.resetIfNewDay(quota);

        verify(domainEventPublisher, never()).publish(any());
        assertThat(quota.getUsedToday()).isEqualTo(10);
    }

    @Test
    void bulkResetPublishesBulkQuotaResetEvent() {
        UserQuota first = UserQuota.builder().id(UUID.randomUUID()).lastResetDate(LocalDate.now(clock).minusDays(1)).build();
        UserQuota second = UserQuota.builder().id(UUID.randomUUID()).lastResetDate(LocalDate.now(clock).minusDays(1)).build();
        when(userQuotaRepository.findAll()).thenReturn(List.of(first, second));
        when(penaltyService.expireFinishedPenalties()).thenReturn(3);

        quotaService.resetAllQuotasAndExpirePenalties();

        ArgumentCaptor<BulkQuotaResetEvent> captor = ArgumentCaptor.forClass(BulkQuotaResetEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().resetCount()).isEqualTo(2);
        assertThat(captor.getValue().expiredPenalties()).isEqualTo(3);
        assertThat(captor.getValue().actor()).isEqualTo(Actor.SYSTEM);
    }
}
