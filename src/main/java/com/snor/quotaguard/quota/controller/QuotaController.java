package com.snor.quotaguard.quota.controller;

import com.snor.quotaguard.quota.dto.response.QuotaResetResponse;
import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import com.snor.quotaguard.quota.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    private static final String QUOTA_EXAMPLE = """
            {
              "id": "0f8b5201-8f39-4ea8-9c52-1e67ef5d00e1",
              "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
              "dailyLimit": 100,
              "usedToday": 35,
              "remainingToday": 65,
              "lastResetDate": "2026-08-02",
              "penaltyLevel": 0
            }
            """;

    private static final String QUOTA_RESET_EXAMPLE = """
            {
              "resetCount": 12,
              "resetDate": "2026-08-02",
              "expiredPenalties": 1
            }
            """;

    private final QuotaService quotaService;

    @Operation(
            summary = "Get the current quota",
            description = "Returns the authenticated user's current quota state and remaining capacity.",
            operationId = "getCurrentQuota"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The user's current quota.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuotaResponse.class),
                            examples = @ExampleObject(name = "currentQuota", summary = "Current quota",
                                    value = QUOTA_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<QuotaResponse> getQuota() {
        return ResponseEntity.ok(quotaService.getCurrentUserQuota());
    }

    @Operation(
            summary = "Reset all quotas (ADMIN)",
            description = """
                    Resets the daily usage of all users, expires active penalties, and publishes a
                    `QuotaResetEvent`.
                    Requires the `ADMIN` role.
                    """,
            operationId = "resetAllQuotas"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All quotas reset and penalties expired.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuotaResetResponse.class),
                            examples = @ExampleObject(name = "quotaReset", summary = "Reset result",
                                    value = QUOTA_RESET_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden")
    })
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuotaResetResponse> resetQuotas() {
        return ResponseEntity.ok(quotaService.resetAllQuotasAndExpirePenalties());
    }
}
