package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record PenaltyExpiredEvent(Instant timestamp, UUID penaltyId) implements DomainEvent {
}
