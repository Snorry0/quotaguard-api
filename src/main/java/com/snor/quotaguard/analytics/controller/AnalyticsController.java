package com.snor.quotaguard.analytics.controller;

import com.snor.quotaguard.analytics.dto.response.UsageStatsResponse;
import com.snor.quotaguard.analytics.dto.response.UsageTrendResponse;
import com.snor.quotaguard.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Analytics",
        description = "Usage statistics, trends, and behavioral insights"
)
@Validated
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final String STATS_EXAMPLE = """
            {
              "from": "2026-07-26",
              "to": "2026-08-02",
              "totalConsumed": 1840,
              "eventCount": 42,
              "averageUsagePerEvent": 43.81,
              "averageDailyUsage": 262.86,
              "overLimitEvents": 2,
              "overLimitFrequency": 0.05,
              "insights": [
                {
                  "label": "Peak usage day",
                  "value": "2026-07-30"
                }
              ]
            }
            """;

    private static final String TREND_EXAMPLE = """
            [
              {
                "date": "2026-08-01",
                "totalConsumed": 210,
                "eventCount": 6
              },
              {
                "date": "2026-08-02",
                "totalConsumed": 180,
                "eventCount": 5
              }
            ]
            """;

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Get usage statistics",
            description = "Returns aggregate usage statistics for the authenticated user over the given number of days.",
            operationId = "getUsageStats"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregate usage statistics.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsageStatsResponse.class),
                            examples = @ExampleObject(name = "usageStats", summary = "Usage statistics",
                                    value = STATS_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/usage")
    public ResponseEntity<UsageStatsResponse> usageStats(
            @RequestParam(defaultValue = "7") @Min(1) @Max(366) int days
    ) {
        return ResponseEntity.ok(analyticsService.getUsageStats(days));
    }

    @Operation(
            summary = "Get usage trend",
            description = "Returns daily usage totals for the authenticated user over the given number of days.",
            operationId = "getUsageTrend"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily usage trend.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsageTrendResponse.class),
                            examples = @ExampleObject(name = "usageTrend", summary = "Daily usage trend",
                                    value = TREND_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/trend")
    public ResponseEntity<List<UsageTrendResponse>> usageTrend(
            @RequestParam(defaultValue = "14") @Min(1) @Max(366) int days
    ) {
        return ResponseEntity.ok(analyticsService.getUsageTrend(days));
    }
}
