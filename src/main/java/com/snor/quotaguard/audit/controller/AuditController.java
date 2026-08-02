package com.snor.quotaguard.audit.controller;

import com.snor.quotaguard.audit.dto.response.AuditEventResponse;
import com.snor.quotaguard.audit.dto.response.PageResponse;
import com.snor.quotaguard.audit.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Audit",
        description = "Read-only audit trail endpoints"
)
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditEventResponse>> getAuditEvents(
            @PageableDefault(size = 20, sort = {"timestamp", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEvents(pageable));
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditEventResponse> getAuditEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(auditService.getAuditEvent(eventId));
    }
}
