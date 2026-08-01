package com.snor.quotaguard.domain;

import com.snor.quotaguard.audit.AuditDetailsConverter;
import com.snor.quotaguard.domain.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_actor_timestamp", columnList = "actor_id,timestamp"),
                @Index(name = "idx_audit_events_timestamp", columnList = "timestamp")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    @Column(nullable = false, length = 64)
    private String resource;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Convert(converter = AuditDetailsConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, String> details;
}
