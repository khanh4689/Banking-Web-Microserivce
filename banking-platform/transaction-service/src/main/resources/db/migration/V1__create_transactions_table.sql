CREATE TABLE transactions (
    id                UUID            PRIMARY KEY,
    idempotency_key   VARCHAR(64)     UNIQUE,
    from_account      VARCHAR(12)     NOT NULL,
    to_account        VARCHAR(12)     NOT NULL,
    amount            NUMERIC(19, 2)  NOT NULL,
    currency          VARCHAR(3)      NOT NULL DEFAULT 'VND',
    status            VARCHAR(10)     NOT NULL DEFAULT 'PENDING',
    failure_reason    TEXT,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tx_from_account ON transactions (from_account);
CREATE INDEX idx_tx_to_account   ON transactions (to_account);
CREATE INDEX idx_tx_idempotency  ON transactions (idempotency_key);
