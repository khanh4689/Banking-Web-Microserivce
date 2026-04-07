CREATE TABLE accounts (
    id              UUID            PRIMARY KEY,
    user_id         VARCHAR(255)    NOT NULL,
    account_number  VARCHAR(12)     NOT NULL UNIQUE,
    balance         NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
