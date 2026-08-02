package com.snor.quotaguard.quota.service;

import com.snor.quotaguard.audit.AuditPublisher;
import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.penalty.service.PenaltyService;
import com.snor.quotaguard.quota.dto.response.QuotaResetResponse;
import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.quota.mapper.UserQuotaMapper;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaRepository userQuotaRepository;
    private final UserQuotaMapper userQuotaMapper;
    private final CurrentUserProvider currentUserProvider;
    private final QuotaGuardProperties properties;
    private final PenaltyService penaltyService;
    private final AuditPublisher auditPublisher;
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
            auditPublisher.publishForCurrentUser(
                    AuditAction.QUOTA_RESET,
                    "QUOTA",
                    quota.getId(),
                    "Quota reset for new day",
                    true
            );
        }
    }

    @Transactional
    public QuotaResetResponse resetAllQuotasAndExpirePenalties() {
        LocalDate today = LocalDate.now(clock);
        var quotas = userQuotaRepository.findAll();
        quotas.forEach(quota -> resetQuota(quota, today));
        userQuotaRepository.saveAll(quotas);
        int expiredPenalties = penaltyService.expireFinishedPenalties();
        QuotaResetResponse response = new QuotaResetResponse(quotas.size(), today, expiredPenalties);
        auditPublisher.publishForCurrentUser(
                AuditAction.QUOTA_RESET,
                "QUOTA",
                null,
                "Quotas reset: " + response.resetCount() + " quotas, " + response.expiredPenalties() + " penalties expired",
                true
        );
        return response;
    }

    private void resetQuota(UserQuota quota, LocalDate resetDate) {
        quota.resetForNewDay(resetDate, properties.penaltyDecayPerReset());
    }
}
