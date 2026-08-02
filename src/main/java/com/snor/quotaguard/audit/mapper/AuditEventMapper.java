package com.snor.quotaguard.audit.mapper;

import com.snor.quotaguard.audit.domain.AuditEvent;
import com.snor.quotaguard.audit.dto.response.AuditEventResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventResponse toResponse(AuditEvent auditEvent);
}
