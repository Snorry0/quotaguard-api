--liquibase formatted sql

--changeset snor:001-create-users
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

                       CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email);

--changeset snor:002-create-user-quotas
CREATE TABLE user_quotas (
                             id UUID PRIMARY KEY,
                             user_id UUID NOT NULL,
                             daily_limit INTEGER NOT NULL,
                             used_today INTEGER NOT NULL,
                             last_reset_date DATE NOT NULL,
                             penalty_level INTEGER NOT NULL,

                             CONSTRAINT uk_user_quotas_user_id UNIQUE (user_id),

                             CONSTRAINT fk_user_quotas_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE
);

--changeset snor:003-create-usage-records
CREATE TABLE usage_records (
                               id UUID PRIMARY KEY,
                               user_id UUID NOT NULL,
                               amount_consumed INTEGER NOT NULL,
                               action_type VARCHAR(64) NOT NULL,
                               occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

                               CONSTRAINT fk_usage_records_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE
);

--changeset snor:004-create-penalty-events
CREATE TABLE penalty_events (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL,
                                type VARCHAR(64) NOT NULL,
                                start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                end_time TIMESTAMP WITHOUT TIME ZONE,
                                active BOOLEAN NOT NULL,

                                CONSTRAINT fk_penalty_events_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

--changeset snor:005-create-usage-sessions
CREATE TABLE usage_sessions (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL,
                                client_reference VARCHAR(128),
                                started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                ended_at TIMESTAMP WITHOUT TIME ZONE,
                                duration_seconds BIGINT,
                                amount_consumed INTEGER,
                                status VARCHAR(32) NOT NULL,
                                metadata TEXT,

                                CONSTRAINT fk_usage_sessions_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);