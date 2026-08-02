package com.snor.quotaguard.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(DomainEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("Failed to dispatch domain event {}", event.getClass().getSimpleName(), ex);
        }
    }
}
