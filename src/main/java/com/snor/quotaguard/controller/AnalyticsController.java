package com.snor.quotaguard.controller;

import com.snor.quotaguard.dto.response.UsageStatsResponse;
import com.snor.quotaguard.dto.response.UsageTrendResponse;
import com.snor.quotaguard.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Analytics",
        description = "Usage statistics, trends, and behavioral insights"
)
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/usage")
    public ResponseEntity<UsageStatsResponse> usageStats(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getUsageStats(days));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<UsageTrendResponse>> usageTrend(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(analyticsService.getUsageTrend(days));
    }
}
