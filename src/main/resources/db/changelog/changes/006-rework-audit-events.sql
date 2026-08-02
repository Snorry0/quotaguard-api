--liquibase formatted sql

--changeset snor:014-rework-audit-events
-- Forward-only, data-preserving conversion of the v1 audit table to v2.
-- Legacy timestamps were written as UTC wall-clock values.
ALTER TABLE audit_events ALTER COLUMN timestamp TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE audit_events RENAME COLUMN resource TO resource_type;

ALTER TABLE audit_events ALTER COLUMN action TYPE VARCHAR(64);
UPDATE audit_events
SET action = CASE action
    WHEN 'CREATE' THEN 'USER_CREATED'
    WHEN 'UPDATE' THEN 'USER_UPDATED'
    WHEN 'DELETE' THEN 'USER_DELETED'
    WHEN 'RESET' THEN 'QUOTA_RESET'
    ELSE action
END;

ALTER TABLE audit_events ADD COLUMN actor_email VARCHAR(320);
ALTER TABLE audit_events ADD COLUMN ip_address VARCHAR(64);
ALTER TABLE audit_events ADD COLUMN success BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE audit_events ADD COLUMN resource_id_uuid UUID;
UPDATE audit_events
SET resource_id_uuid = CASE
    WHEN resource_id LIKE '________-____-____-____-____________' THEN CAST(resource_id AS UUID)
    ELSE NULL
END;
ALTER TABLE audit_events DROP COLUMN resource_id;
ALTER TABLE audit_events RENAME COLUMN resource_id_uuid TO resource_id;

ALTER TABLE audit_events ADD COLUMN description TEXT;
UPDATE audit_events
SET description = 'Legacy audit event: ' || COALESCE(details, '')
WHERE description IS NULL;
ALTER TABLE audit_events ALTER COLUMN description SET NOT NULL;

ALTER TABLE audit_events DROP COLUMN details;

DROP INDEX IF EXISTS idx_audit_events_actor_timestamp;
DROP INDEX IF EXISTS idx_audit_events_timestamp;

--changeset snor:015-audit-events-indexes
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_events_actor_timestamp ON audit_events(actor_id, timestamp);
CREATE INDEX idx_audit_events_resource_type_timestamp ON audit_events(resource_type, timestamp);
