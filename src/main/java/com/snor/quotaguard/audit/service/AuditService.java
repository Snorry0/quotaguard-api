package com.snor.quotaguard.audit.service;

import com.snor.quotaguard.audit.dto.response.AuditEventResponse;
import com.snor.quotaguard.audit.dto.response.PageResponse;
import com.snor.quotaguard.audit.mapper.AuditEventMapper;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("timestamp", "id", "action", "resourceType", "actorEmail");

    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> getAuditEvents(Pageable pageable) {
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                sanitizeSort(pageable.getSort())
        );
        return PageResponse.of(auditEventRepository.findAll(safePageable).map(auditEventMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getAuditEvent(UUID eventId) {
        return auditEventRepository.findById(eventId)
                .map(auditEventMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found"));
    }

    private Sort sanitizeSort(Sort sort) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported sort property: " + order.getProperty());
            }
            orders.add(order);
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
