package com.snor.quotaguard.penalty.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.penalty.mapper.PenaltyEventMapper;
import com.snor.quotaguard.penalty.repository.PenaltyEventRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PenaltyServicePolicyTest {

    private final PenaltyService penaltyService = new PenaltyService(
            mock(PenaltyEventRepository.class),
            mock(PenaltyEventMapper.class),
            mock(CurrentUserProvider.class),
            mock(DomainEventPublisher.class),
            new QuotaGuardProperties(100, 1, new QuotaGuardProperties.Penalties(Duration.ofMinutes(15), Duration.ofHours(4)), new QuotaGuardProperties.Sessions(1, 1)),
            Clock.systemUTC()
    );

    @Test
    void resolvesProgressivePenaltyTypes() {
        assertThat(penaltyService.resolvePenaltyType(1)).isEqualTo(PenaltyType.WARNING);
        assertThat(penaltyService.resolvePenaltyType(2)).isEqualTo(PenaltyType.SHORT_COOLDOWN);
        assertThat(penaltyService.resolvePenaltyType(3)).isEqualTo(PenaltyType.LONG_COOLDOWN);
        assertThat(penaltyService.resolvePenaltyType(10)).isEqualTo(PenaltyType.LONG_COOLDOWN);
    }
}
