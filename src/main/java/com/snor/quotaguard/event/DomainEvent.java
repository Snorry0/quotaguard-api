package com.snor.quotaguard.event;

import java.time.Instant;

public interface DomainEvent {

    Instant timestamp();
}
