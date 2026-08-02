package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(Instant timestamp, Actor actor, UUID userId) implements DomainEvent {
}
