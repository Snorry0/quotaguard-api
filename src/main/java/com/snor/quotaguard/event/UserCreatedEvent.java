package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(Instant timestamp, Actor actor, UUID userId, String email, String role) implements DomainEvent {
}
