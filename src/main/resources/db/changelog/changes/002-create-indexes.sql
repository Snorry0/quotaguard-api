--liquibase formatted sql

--changeset snor:006-create-usage-records-indexes
CREATE INDEX idx_usage_user_timestamp ON usage_records(user_id, occurred_at);
CREATE INDEX idx_usage_action_type ON usage_records(action_type);

--changeset snor:007-create-penalty-events-indexes
CREATE INDEX idx_penalty_user_start ON penalty_events(user_id, start_time);
CREATE INDEX idx_penalty_active_end ON penalty_events(active, end_time);

--changeset snor:008-create-usage-sessions-indexes
CREATE INDEX idx_session_user_status_started ON usage_sessions(user_id, status, started_at);
CREATE INDEX idx_session_user_started ON usage_sessions(user_id, started_at);
