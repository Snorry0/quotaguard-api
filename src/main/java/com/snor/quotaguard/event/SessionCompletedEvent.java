package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record SessionCompletedEvent(Instant timestamp, Actor actor, UUID sessionId) implements DomainEvent {
}
