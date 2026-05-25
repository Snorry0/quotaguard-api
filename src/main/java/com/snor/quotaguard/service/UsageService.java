package com.snor.quotaguard.service;

import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.dto.response.ConsumeUsageResponse;
import com.snor.quotaguard.dto.response.UsageRecordResponse;
import com.snor.quotaguard.exception.ActivePenaltyException;
import com.snor.quotaguard.exception.QuotaExceededException;
import com.snor.quotaguard.mapper.UsageRecordMapper;
import com.snor.quotaguard.mapper.UserQuotaMapper;
import com.snor.quotaguard.repository.UsageRecordRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsageService {

    private static final int MAX_HISTORY_PAGE_SIZE = 100;

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
        UserQuota quota = quotaService.getQuotaForUpdate(user);
        quotaService.resetIfNewDay(quota);
        penaltyService.expireFinishedPenalties();

        penaltyService.findActiveBlockingPenalty(user)
                .ifPresent(activePenalty -> {
                    throw new ActivePenaltyException(
                            activePenalty.getType(),
                            activePenalty.getEndTime()
                    );
                });

        int projectedUsage = quota.getUsedToday() + request.amountConsumed();

        if (projectedUsage > quota.getDailyLimit()) {
            PenaltyEvent penalty = penaltyService.applyQuotaViolation(user, quota);

            throw new QuotaExceededException(
                    quota.getDailyLimit(),
                    quota.getUsedToday(),
                    request.amountConsumed(),
                    penalty.getType()
            );
        }

        quota.setUsedToday(projectedUsage);

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

    @Transactional(readOnly = true)
    public Page<UsageRecordResponse> getHistory(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_HISTORY_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return usageRecordRepository.findByUser(currentUserProvider.getCurrentUser(), pageable)
                .map(usageRecordMapper::toResponse);
    }
}
