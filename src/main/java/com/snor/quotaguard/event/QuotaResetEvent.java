package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record QuotaResetEvent(
        Instant timestamp,
        Actor actor,
        UUID quotaId
) implements DomainEvent {
}
