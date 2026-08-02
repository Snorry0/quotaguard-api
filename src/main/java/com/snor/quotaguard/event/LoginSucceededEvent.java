package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.UUID;

public record LoginSucceededEvent(Instant timestamp, Actor actor, UUID userId, String email) implements DomainEvent {
}
