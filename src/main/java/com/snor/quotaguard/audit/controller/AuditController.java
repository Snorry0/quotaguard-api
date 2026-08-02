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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Audit",
        description = "Read-only audit trail endpoints"
)
@Validated
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Lists audit events, paginated and sorted.
     *
     * <p>Design note (sort validation): {@code @AllowedSortField} is deliberately
     * NOT applied to a raw {@code ?sort=} parameter here. The API exposes the
     * compound Spring format {@code ?sort=property[,direction]} (e.g.
     * {@code ?sort=timestamp,desc}), while {@code @AllowedSortField} matches a
     * single property name exactly &mdash; applying it to the raw value would
     * reject every legitimate multi-property/direction sort and break existing
     * callers. It would also break requests that omit {@code ?sort=} entirely:
     * a required or null-rejecting sort param would turn the admin no-param
     * 200-with-defaults into a 400 (the non-admin no-param 403 is unaffected
     * because method-security runs before MVC param binding). The
     * whitelist is therefore enforced where {@code Sort} is
     * actually resolved: {@code AuditService.sanitizeSort}, using the exact
     * corrected JPA entity property names
     * {@code {timestamp, id, action, resourceType, actorEmail}}; an unknown
     * property surfaces as 400 through the {@code IllegalArgumentException}
     * handler. {@code @PageableDefault} keeps {@code timestamp, id desc} when
     * {@code ?sort=} is absent. Contract note: {@code ?sort=foo} and
     * {@code ?sort=unknownProperty,asc} are rejected with 400 (unchanged);
     * no-param requests keep their default sort (unchanged).</p>
     *
     * @param pageable page/size/sort/direction, defaults {@code timestamp, id desc}
     */
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
