package com.snor.quotaguard.audit.service;

import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.domain.AuditEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.AuditAction;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    @Transactional
    public void record(AuditAction action, String resource, String resourceId, Map<String, String> details) {
        UUID actorId = currentUserProvider.getCurrentUserIfPresent()
                .map(User::getId)
                .orElse(null);

        auditEventRepository.save(AuditEvent.builder()
                .timestamp(LocalDateTime.now(clock))
                .actorId(actorId)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .details(details)
                .build());
    }
}
