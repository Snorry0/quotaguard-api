package com.snor.quotaguard.audit.domain;

import jakarta.persistence.Column;
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

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_events_actor_timestamp", columnList = "actor_id,timestamp"),
                @Index(name = "idx_audit_events_resource_type_timestamp", columnList = "resource_type,timestamp")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditAction action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;
}
