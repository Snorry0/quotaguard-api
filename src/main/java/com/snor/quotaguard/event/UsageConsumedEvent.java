package com.snor.quotaguard.event;

import com.snor.quotaguard.domain.enums.ActionType;

import java.time.Instant;
import java.util.UUID;

public record UsageConsumedEvent(
        Instant timestamp,
        Actor actor,
        UUID recordId,
        int amountConsumed,
        ActionType actionType
) implements DomainEvent {
}
