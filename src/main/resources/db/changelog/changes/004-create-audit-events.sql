--liquibase formatted sql

--changeset snor:012-create-audit-events
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    actor_id UUID,
    action VARCHAR(32) NOT NULL,
    resource VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    details TEXT
);

CREATE INDEX idx_audit_events_actor_timestamp ON audit_events(actor_id, timestamp);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
