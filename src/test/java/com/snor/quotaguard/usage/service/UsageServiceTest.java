package com.snor.quotaguard.usage.service;

import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.ActionType;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.UsageConsumedEvent;
import com.snor.quotaguard.exception.QuotaExceededException;
import com.snor.quotaguard.penalty.service.PenaltyService;
import com.snor.quotaguard.quota.mapper.UserQuotaMapper;
import com.snor.quotaguard.quota.service.QuotaService;
import com.snor.quotaguard.security.CurrentUserProvider;
import com.snor.quotaguard.usage.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.usage.mapper.UsageRecordMapper;
import com.snor.quotaguard.usage.repository.UsageRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageServiceTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final QuotaService quotaService = mock(QuotaService.class);
    private final PenaltyService penaltyService = mock(PenaltyService.class);
    private final UsageRecordRepository usageRecordRepository = mock(UsageRecordRepository.class);
    private final UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
    private final UserQuotaMapper userQuotaMapper = mock(UserQuotaMapper.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);

    private final UsageService usageService = new UsageService(
            currentUserProvider,
            quotaService,
            penaltyService,
            usageRecordRepository,
            usageRecordMapper,
            userQuotaMapper,
            domainEventPublisher,
            clock
    );

    @Test
    void successfulConsumptionPublishesUsageConsumedEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        UserQuota quota = UserQuota.builder().dailyLimit(100).usedToday(10).build();
        when(quotaService.getQuotaForUpdate(user)).thenReturn(quota);
        UsageRecord record = UsageRecord.builder()
                .id(UUID.randomUUID())
                .amountConsumed(5)
                .actionType(ActionType.API_CALL)
                .build();
        when(usageRecordRepository.save(any(UsageRecord.class))).thenReturn(record);

        usageService.consumeForUser(user, new ConsumeUsageRequest(5, ActionType.API_CALL));

        ArgumentCaptor<UsageConsumedEvent> captor = ArgumentCaptor.forClass(UsageConsumedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().recordId()).isEqualTo(record.getId());
        assertThat(captor.getValue().amountConsumed()).isEqualTo(5);
        assertThat(captor.getValue().actionType()).isEqualTo(ActionType.API_CALL);
    }

    @Test
    void rejectedConsumptionDoesNotPublishEvent() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        UserQuota quota = UserQuota.builder().dailyLimit(10).usedToday(10).build();
        when(quotaService.getQuotaForUpdate(user)).thenReturn(quota);
        PenaltyEvent penalty = PenaltyEvent.builder()
                .id(UUID.randomUUID())
                .type(PenaltyType.WARNING)
                .build();
        when(penaltyService.applyQuotaViolation(user, quota)).thenReturn(penalty);

        assertThatThrownBy(() -> usageService.consumeForUser(
                user, new ConsumeUsageRequest(5, ActionType.API_CALL)))
                .isInstanceOf(QuotaExceededException.class);

        verify(domainEventPublisher, never()).publish(any());
    }
}
