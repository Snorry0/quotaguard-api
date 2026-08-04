--liquibase formatted sql

--changeset snor:016-create-refresh-tokens
CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY,
    token_hash    VARCHAR(64)  NOT NULL,
    user_id       UUID         NOT NULL,
    family_id     UUID         NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_used_at  TIMESTAMP WITHOUT TIME ZONE,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id  ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family   ON refresh_tokens(family_id);
