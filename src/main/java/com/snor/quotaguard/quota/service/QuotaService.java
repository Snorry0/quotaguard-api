package com.snor.quotaguard.quota.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.BulkQuotaResetEvent;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.QuotaResetEvent;
import com.snor.quotaguard.penalty.service.PenaltyService;
import com.snor.quotaguard.quota.dto.response.QuotaResetResponse;
import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.quota.mapper.UserQuotaMapper;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaRepository userQuotaRepository;
    private final UserQuotaMapper userQuotaMapper;
    private final CurrentUserProvider currentUserProvider;
    private final QuotaGuardProperties properties;
    private final PenaltyService penaltyService;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Transactional
    public QuotaResponse getCurrentUserQuota() {
        User user = currentUserProvider.getCurrentUser();
        UserQuota quota = userQuotaRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Quota not found for current user"));
        resetIfNewDay(quota);
        return userQuotaMapper.toResponse(quota);
    }

    @Transactional
    public UserQuota getQuotaForUpdate(User user) {
        return userQuotaRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quota not found for user"));
    }

    @Transactional
    public void resetIfNewDay(UserQuota quota) {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(quota.getLastResetDate())) {
            resetQuota(quota, today);
            domainEventPublisher.publish(new QuotaResetEvent(
                    Instant.now(clock),
                    Actor.of(currentUserProvider.getCurrentUserIfPresent()),
                    quota.getId()
            ));
        }
    }

    @Timed(value = "quotaguard.timer.quota.reset", percentiles = {0.5, 0.95, 0.99})
    @Transactional
    public QuotaResetResponse resetAllQuotasAndExpirePenalties() {
        LocalDate today = LocalDate.now(clock);
        var quotas = userQuotaRepository.findAll();
        quotas.forEach(quota -> resetQuota(quota, today));
        userQuotaRepository.saveAll(quotas);
        int expiredPenalties = penaltyService.expireFinishedPenalties();
        QuotaResetResponse response = new QuotaResetResponse(quotas.size(), today, expiredPenalties);
        domainEventPublisher.publish(new BulkQuotaResetEvent(
                Instant.now(clock),
                Actor.of(currentUserProvider.getCurrentUserIfPresent()),
                response.resetCount(),
                response.expiredPenalties()
        ));
        return response;
    }

    private void resetQuota(UserQuota quota, LocalDate resetDate) {
        quota.resetForNewDay(resetDate, properties.penaltyDecayPerReset());
    }
}
