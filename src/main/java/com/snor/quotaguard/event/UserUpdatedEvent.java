package com.snor.quotaguard.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record UserUpdatedEvent(
        Instant timestamp,
        Actor actor,
        UUID userId,
        Set<String> changedFields
) implements DomainEvent {

    public UserUpdatedEvent {
        changedFields = Collections.unmodifiableSet(new LinkedHashSet<>(changedFields));
    }
}
