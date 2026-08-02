package com.snor.quotaguard.event;

import com.snor.quotaguard.domain.enums.PenaltyType;

import java.time.Instant;
import java.util.UUID;

public record PenaltyAppliedEvent(
        Instant timestamp,
        Actor actor,
        UUID penaltyId,
        PenaltyType type
) implements DomainEvent {
}
