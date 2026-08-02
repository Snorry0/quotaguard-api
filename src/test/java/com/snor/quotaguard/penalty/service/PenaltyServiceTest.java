package com.snor.quotaguard.penalty.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.PenaltyAppliedEvent;
import com.snor.quotaguard.event.PenaltyExpiredEvent;
import com.snor.quotaguard.penalty.mapper.PenaltyEventMapper;
import com.snor.quotaguard.penalty.repository.PenaltyEventRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PenaltyServiceTest {

    private final PenaltyEventRepository penaltyEventRepository = mock(PenaltyEventRepository.class);
    private final PenaltyEventMapper penaltyEventMapper = mock(PenaltyEventMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);
    private final QuotaGuardProperties properties = new QuotaGuardProperties(
            100,
            1,
            new QuotaGuardProperties.Penalties(Duration.ofMinutes(15), Duration.ofHours(4)),
            new QuotaGuardProperties.Sessions(1, 1)
    );

    private final PenaltyService penaltyService = new PenaltyService(
            penaltyEventRepository,
            penaltyEventMapper,
            currentUserProvider,
            domainEventPublisher,
            properties,
            clock
    );

    @Test
    void quotaViolationPublishesPenaltyAppliedEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        UserQuota quota = UserQuota.builder().penaltyLevel(0).build();
        PenaltyEvent penalty = PenaltyEvent.builder()
                .id(UUID.randomUUID())
                .type(PenaltyType.WARNING)
                .build();
        when(penaltyEventRepository.save(any(PenaltyEvent.class))).thenReturn(penalty);

        penaltyService.applyQuotaViolation(user, quota);

        ArgumentCaptor<PenaltyAppliedEvent> captor = ArgumentCaptor.forClass(PenaltyAppliedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().penaltyId()).isEqualTo(penalty.getId());
        assertThat(captor.getValue().type()).isEqualTo(PenaltyType.WARNING);
        assertThat(captor.getValue().actor()).isEqualTo(new Actor(user.getId(), user.getEmail()));
    }

    @Test
    void expiryPublishesPenaltyExpiredEventPerPenalty() {
        LocalDateTime now = LocalDateTime.now(clock);
        PenaltyEvent expired = PenaltyEvent.builder()
                .id(UUID.randomUUID())
                .active(true)
                .build();
        when(penaltyEventRepository.findByActiveTrueAndEndTimeBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        penaltyService.expireFinishedPenalties();

        ArgumentCaptor<PenaltyExpiredEvent> captor = ArgumentCaptor.forClass(PenaltyExpiredEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().penaltyId()).isEqualTo(expired.getId());
        assertThat(expired.isActive()).isFalse();
    }
}
