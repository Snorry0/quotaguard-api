package com.snor.quotaguard.usage.service;

import com.snor.quotaguard.penalty.service.PenaltyService;

import com.snor.quotaguard.quota.service.QuotaService;

import com.snor.quotaguard.common.PageRequestFactory;
import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.usage.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.usage.dto.response.ConsumeUsageResponse;
import com.snor.quotaguard.usage.dto.response.UsageRecordResponse;
import com.snor.quotaguard.exception.ActivePenaltyException;
import com.snor.quotaguard.exception.QuotaExceededException;
import com.snor.quotaguard.usage.mapper.UsageRecordMapper;
import com.snor.quotaguard.quota.mapper.UserQuotaMapper;
import com.snor.quotaguard.usage.repository.UsageRecordRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final CurrentUserProvider currentUserProvider;
    private final QuotaService quotaService;
    private final PenaltyService penaltyService;
    private final UsageRecordRepository usageRecordRepository;
    private final UsageRecordMapper usageRecordMapper;
    private final UserQuotaMapper userQuotaMapper;
    private final Clock clock;

    @Transactional(noRollbackFor = {
            QuotaExceededException.class,
            ActivePenaltyException.class
    })
    public ConsumeUsageResponse consume(ConsumeUsageRequest request) {
        return consumeForUser(currentUserProvider.getCurrentUser(), request);
    }

    @Transactional(noRollbackFor = {
            QuotaExceededException.class,
            ActivePenaltyException.class
    })
    public ConsumeUsageResponse consumeForUser(User user, ConsumeUsageRequest request) {
        UserQuota quota = prepareForConsumption(user);

        if (!quota.canConsume(request.amountConsumed())) {
            PenaltyEvent penalty = penaltyService.applyQuotaViolation(user, quota);

            throw new QuotaExceededException(
                    quota.getDailyLimit(),
                    quota.getUsedToday(),
                    request.amountConsumed(),
                    penalty.getType()
            );
        }

        quota.consume(request.amountConsumed());

        UsageRecord record = usageRecordRepository.save(UsageRecord.builder()
                .user(user)
                .amountConsumed(request.amountConsumed())
                .actionType(request.actionType())
                .timestamp(LocalDateTime.now(clock))
                .build());

        return new ConsumeUsageResponse(
                usageRecordMapper.toResponse(record),
                userQuotaMapper.toResponse(quota)
        );
    }

    @Transactional
    public UserQuota prepareForConsumption(User user) {
        UserQuota quota = quotaService.getQuotaForUpdate(user);
        quotaService.resetIfNewDay(quota);
        penaltyService.expireFinishedPenalties();
        penaltyService.ensureNoBlockingPenalty(user);
        return quota;
    }

    @Transactional(readOnly = true)
    public Page<UsageRecordResponse> getHistory(int page, int size) {
        Pageable pageable = PageRequestFactory.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return usageRecordRepository.findByUser(currentUserProvider.getCurrentUser(), pageable)
                .map(usageRecordMapper::toResponse);
    }
}
