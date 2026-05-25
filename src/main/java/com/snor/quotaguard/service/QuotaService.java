package com.snor.quotaguard.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.dto.response.QuotaResetResponse;
import com.snor.quotaguard.dto.response.QuotaResponse;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.mapper.UserQuotaMapper;
import com.snor.quotaguard.repository.UserQuotaRepository;
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
        }
    }

    @Transactional
    public QuotaResetResponse resetAllQuotas() {
        LocalDate today = LocalDate.now(clock);
        var quotas = userQuotaRepository.findAll();
        quotas.forEach(quota -> resetQuota(quota, today));
        userQuotaRepository.saveAll(quotas);
        return new QuotaResetResponse(quotas.size(), today);
    }

    private void resetQuota(UserQuota quota, LocalDate resetDate) {
        quota.setUsedToday(0);
        quota.setLastResetDate(resetDate);
        int decayedLevel = Math.max(0, quota.getPenaltyLevel() - properties.penaltyDecayPerReset());
        quota.setPenaltyLevel(decayedLevel);
    }
}
