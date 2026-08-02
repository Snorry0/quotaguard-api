package com.snor.quotaguard.audit.service;

import com.snor.quotaguard.audit.AuditCommand;
import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.audit.domain.AuditEvent;
import com.snor.quotaguard.audit.dto.response.AuditEventResponse;
import com.snor.quotaguard.audit.mapper.AuditEventMapper;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private final AuditEventWriter auditEventWriter = mock(AuditEventWriter.class);
    private final AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
    private final AuditEventMapper auditEventMapper = mock(AuditEventMapper.class);
    private final AuditService auditService = new AuditService(auditEventWriter, auditEventRepository, auditEventMapper);

    @Test
    void committedListenerPersistsSuccessfulEventsOnly() {
        auditService.onAuditCommandCommitted(sampleCommand(true));
        verify(auditEventWriter).persist(any(AuditCommand.class));

        org.mockito.Mockito.clearInvocations(auditEventWriter);
        auditService.onAuditCommandCommitted(sampleCommand(false));
        verify(auditEventWriter, never()).persist(any(AuditCommand.class));
    }

    @Test
    void completionListenerPersistsFailedEventsOnly() {
        auditService.onAuditCommandFailed(sampleCommand(false));
        verify(auditEventWriter).persist(any(AuditCommand.class));

        org.mockito.Mockito.clearInvocations(auditEventWriter);
        auditService.onAuditCommandFailed(sampleCommand(true));
        verify(auditEventWriter, never()).persist(any(AuditCommand.class));
    }

    @Test
    void listenerSwallowsPersistenceFailure() {
        org.mockito.Mockito.doThrow(new RuntimeException("database is down"))
                .when(auditEventWriter).persist(any(AuditCommand.class));

        assertThatCode(() -> auditService.onAuditCommandCommitted(sampleCommand(true)))
                .doesNotThrowAnyException();
        assertThatCode(() -> auditService.onAuditCommandFailed(sampleCommand(false)))
                .doesNotThrowAnyException();
    }

    @Test
    void getAuditEventReturnsMappedEvent() {
        AuditEvent event = AuditEvent.builder().id(UUID.randomUUID()).build();
        when(auditEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        AuditEventResponse response = new AuditEventResponse(
                event.getId(), null, null, null, null, null, null, null, null, false
        );
        when(auditEventMapper.toResponse(event)).thenReturn(response);

        assertThat(auditService.getAuditEvent(event.getId())).isEqualTo(response);
    }

    @Test
    void getAuditEventThrowsWhenMissing() {
        when(auditEventRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditEvent(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAuditEventsClampsPageSizeToOneHundred() {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        when(auditEventRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        auditService.getAuditEvents(PageRequest.of(0, 5000, sort));

        verify(auditEventRepository).findAll(PageRequest.of(0, 100, sort));
    }

    @Test
    void getAuditEventsRejectsUnsupportedSortProperty() {
        when(auditEventRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        assertThatThrownBy(() -> auditService.getAuditEvents(
                PageRequest.of(0, 20, Sort.by("unknownProperty"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AuditCommand sampleCommand(boolean success) {
        return new AuditCommand(
                Instant.parse("2026-08-02T10:00:00Z"),
                AuditAction.USER_CREATED,
                UUID.randomUUID(),
                "admin@example.com",
                "USER",
                UUID.randomUUID(),
                "Admin created user",
                "127.0.0.1",
                success
        );
    }
}
