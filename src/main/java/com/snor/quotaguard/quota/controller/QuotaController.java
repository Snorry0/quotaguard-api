package com.snor.quotaguard.quota.controller;

import com.snor.quotaguard.quota.dto.response.QuotaResetResponse;
import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import com.snor.quotaguard.quota.service.QuotaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Quota",
        description = "Current quota state and quota reset endpoints"
)
@RestController
@RequestMapping("/api/v1/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    @GetMapping
    public ResponseEntity<QuotaResponse> getQuota() {
        return ResponseEntity.ok(quotaService.getCurrentUserQuota());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuotaResetResponse> resetQuotas() {
        return ResponseEntity.ok(quotaService.resetAllQuotasAndExpirePenalties());
    }
}
