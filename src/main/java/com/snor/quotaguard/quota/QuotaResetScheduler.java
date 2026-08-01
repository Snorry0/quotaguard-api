package com.snor.quotaguard.quota;

import com.snor.quotaguard.quota.service.QuotaService;

import com.snor.quotaguard.quota.dto.response.QuotaResetResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuotaResetScheduler.class);

    private final QuotaService quotaService;

    @Scheduled(cron = "${quotaguard.reset-cron}")
    public void resetDailyQuotas() {
        QuotaResetResponse response = quotaService.resetAllQuotasAndExpirePenalties();
        log.info("Daily quota reset completed. resetCount={}, expiredPenalties={}", response.resetCount(), response.expiredPenalties());
    }
}
