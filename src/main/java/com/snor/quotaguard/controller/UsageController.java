package com.snor.quotaguard.controller;

import com.snor.quotaguard.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.dto.response.ConsumeUsageResponse;
import com.snor.quotaguard.dto.response.UsageRecordResponse;
import com.snor.quotaguard.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Usage",
        description = "Resource consumption and usage history endpoints"
)
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @Operation(
            summary = "Consume resource units",
            description = """
                    Attempts to consume resource units for the authenticated user.
                    If the user exceeds the daily quota, a progressive penalty may be applied.
                    """
    )
    @PostMapping("/consume")
    public ResponseEntity<ConsumeUsageResponse> consume(
            @Valid @RequestBody ConsumeUsageRequest request
    ) {
        return ResponseEntity.ok(usageService.consume(request));
    }

    @Operation(
            summary = "Get usage history",
            description = "Returns the authenticated user's historical usage records."
    )
    @GetMapping("/history")
    public ResponseEntity<Page<UsageRecordResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(usageService.getHistory(page, size));
    }
}
