package com.snor.quotaguard.controller;

import com.snor.quotaguard.dto.response.QuotaResetResponse;
import com.snor.quotaguard.dto.response.QuotaResponse;
import com.snor.quotaguard.service.PenaltyService;
import com.snor.quotaguard.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;
    private final PenaltyService penaltyService;

    @GetMapping
    public ResponseEntity<QuotaResponse> getQuota() {
        return ResponseEntity.ok(quotaService.getCurrentUserQuota());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuotaResetResponse> resetQuotas() {
        QuotaResetResponse response = quotaService.resetAllQuotas();
        penaltyService.expireFinishedPenalties();
        return ResponseEntity.ok(response);
    }
}
