package com.snor.quotaguard.audit;

import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public void publishForCurrentUser(
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String description,
            boolean success
    ) {
        User actor = currentUserProvider.getCurrentUserIfPresent().orElse(null);
        publishWithActor(
                action,
                resourceType,
                resourceId,
                description,
                success,
                actor != null ? actor.getId() : null,
                actor != null ? actor.getEmail() : null
        );
    }

    public void publishWithActor(
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String description,
            boolean success,
            UUID actorId,
            String actorEmail
    ) {
        try {
            eventPublisher.publishEvent(new AuditCommand(
                    Instant.now(clock),
                    action,
                    actorId,
                    actorEmail,
                    resourceType,
                    resourceId,
                    description,
                    resolveIpAddress(),
                    success
            ));
        } catch (Exception ex) {
            log.error("Failed to dispatch audit event for action {}: {}", action, ex.getMessage());
        }
    }

    private String resolveIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
