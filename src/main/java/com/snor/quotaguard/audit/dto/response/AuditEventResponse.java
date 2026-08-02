package com.snor.quotaguard.audit.dto.response;

import com.snor.quotaguard.audit.domain.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant timestamp,
        UUID actorId,
        String actorEmail,
        AuditAction action,
        String resourceType,
        UUID resourceId,
        String description,
        String ipAddress,
        boolean success
) {
}
