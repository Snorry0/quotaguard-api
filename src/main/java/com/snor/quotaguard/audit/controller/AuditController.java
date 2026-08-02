package com.snor.quotaguard.audit.controller;

import com.snor.quotaguard.audit.dto.response.AuditEventResponse;
import com.snor.quotaguard.audit.dto.response.PageResponse;
import com.snor.quotaguard.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    private static final String AUDIT_EVENT_EXAMPLE = """
            {
              "id": "d4e5f6a7-0000-4000-8000-000000000004",
              "timestamp": "2026-08-02T08:30:00Z",
              "actorId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
              "actorEmail": "demo@example.com",
              "action": "USER_CREATED",
              "resourceType": "USER",
              "resourceId": "e5f6a7b8-0000-4000-8000-000000000005",
              "description": "User account created",
              "ipAddress": "192.168.1.42",
              "success": true
            }
            """;

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
    @Operation(
            summary = "List audit events (ADMIN)",
            description = """
                    Returns a page of audit events, sorted by the `sort` query parameter
                    (default `timestamp,id` descending). Unknown sort properties return 400.
                    Requires the `ADMIN` role.
                    """,
            operationId = "getAuditEvents"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of audit events.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden")
    })
    @Parameters({
            @Parameter(ref = "Sort")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditEventResponse>> getAuditEvents(
            @PageableDefault(size = 20, sort = {"timestamp", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEvents(pageable));
    }

    @Operation(
            summary = "Get an audit event by ID (ADMIN)",
            description = "Returns a single audit event by its UUID identifier. Requires the `ADMIN` role.",
            operationId = "getAuditEvent"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested audit event.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditEventResponse.class),
                            examples = @ExampleObject(name = "auditEvent", summary = "Audit event",
                                    value = AUDIT_EVENT_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden"),
            @ApiResponse(responseCode = "404", ref = "NotFound")
    })
    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditEventResponse> getAuditEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(auditService.getAuditEvent(eventId));
    }
}
