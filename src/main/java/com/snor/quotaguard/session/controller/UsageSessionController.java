package com.snor.quotaguard.session.controller;

import com.snor.quotaguard.session.dto.request.EndUsageSessionRequest;
import com.snor.quotaguard.session.dto.request.StartUsageSessionRequest;
import com.snor.quotaguard.session.dto.response.EndUsageSessionResponse;
import com.snor.quotaguard.session.dto.response.UsageSessionResponse;
import com.snor.quotaguard.session.service.UsageSessionService;
import com.snor.quotaguard.validation.annotation.AllowedPageSize;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Sessions",
        description = "Session lifecycle tracking and duration-based quota consumption"
)
@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class UsageSessionController {

    private final UsageSessionService usageSessionService;

    @PostMapping("/start")
    public ResponseEntity<UsageSessionResponse> startSession(
            @Valid @RequestBody StartUsageSessionRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usageSessionService.startSession(request));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<EndUsageSessionResponse> endSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody EndUsageSessionRequest request
    ) {
        return ResponseEntity.ok(usageSessionService.endSession(sessionId, request));
    }

    @GetMapping("/active")
    public ResponseEntity<UsageSessionResponse> activeSession() {
        return usageSessionService.getActiveSession()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<UsageSessionResponse>> history(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @AllowedPageSize int size
    ) {
        return ResponseEntity.ok(usageSessionService.getHistory(page, size));
    }
}