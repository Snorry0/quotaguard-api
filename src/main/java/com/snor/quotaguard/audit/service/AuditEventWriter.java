package com.snor.quotaguard.audit.service;

import com.snor.quotaguard.audit.AuditCommand;
import com.snor.quotaguard.audit.domain.AuditEvent;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditEventWriter {

    private final AuditEventRepository auditEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AuditCommand command) {
        auditEventRepository.save(AuditEvent.builder()
                .timestamp(command.timestamp())
                .actorId(command.actorId())
                .actorEmail(command.actorEmail())
                .action(command.action())
                .resourceType(command.resourceType())
                .resourceId(command.resourceId())
                .description(command.description())
                .ipAddress(command.ipAddress())
                .success(command.success())
                .build());
    }
}
