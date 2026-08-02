package com.snor.quotaguard.audit;

import com.snor.quotaguard.audit.domain.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditCommand(
        Instant timestamp,
        AuditAction action,
        UUID actorId,
        String actorEmail,
        String resourceType,
        UUID resourceId,
        String description,
        String ipAddress,
        boolean success
) {
}
