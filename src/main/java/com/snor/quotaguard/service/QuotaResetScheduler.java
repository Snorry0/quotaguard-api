package com.snor.quotaguard.service;

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
    private final PenaltyService penaltyService;

    @Scheduled(cron = "${quotaguard.reset-cron}")
    public void resetDailyQuotas() {
        var response = quotaService.resetAllQuotas();
        int expiredPenalties = penaltyService.expireFinishedPenalties();
        log.info("Daily quota reset completed. resetCount={}, expiredPenalties={}", response.resetCount(), expiredPenalties);
    }
}
