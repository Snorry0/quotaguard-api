--liquibase formatted sql

--changeset snor:009-add-version-to-user-quotas
ALTER TABLE user_quotas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--changeset snor:010-add-version-to-usage-sessions
ALTER TABLE usage_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--changeset snor:011-add-version-to-penalty-events
ALTER TABLE penalty_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
