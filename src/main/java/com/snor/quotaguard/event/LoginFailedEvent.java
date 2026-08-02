package com.snor.quotaguard.event;

import java.time.Instant;

public record LoginFailedEvent(
        Instant timestamp,
        String attemptedEmail
) implements DomainEvent {
}
