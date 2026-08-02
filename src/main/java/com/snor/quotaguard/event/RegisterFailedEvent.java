package com.snor.quotaguard.event;

import java.time.Instant;

public record RegisterFailedEvent(
        Instant timestamp,
        String attemptedEmail
) implements DomainEvent {
}
