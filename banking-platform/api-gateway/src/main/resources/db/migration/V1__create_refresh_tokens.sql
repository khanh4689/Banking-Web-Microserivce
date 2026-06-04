CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    replaced_by_token VARCHAR NULL
);

CREATE INDEX idx_refresh_tokens_user_id
ON refresh_tokens(user_id);