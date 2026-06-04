CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR NOT NULL UNIQUE,
    user_id VARCHAR NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_tokens_token ON email_verification_tokens (token);
CREATE INDEX idx_email_tokens_user_id ON email_verification_tokens (user_id);
