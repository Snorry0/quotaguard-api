package com.snor.quotaguard.event;

import java.time.Instant;

public record BulkQuotaResetEvent(
        Instant timestamp,
        Actor actor,
        int resetCount,
        int expiredPenalties
) implements DomainEvent {
}
