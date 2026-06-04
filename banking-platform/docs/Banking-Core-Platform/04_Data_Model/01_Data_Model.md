# Banking Core Platform
## Document 04 — Data Model

**Document ID:** BCP-DM-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team / Solution Architecture  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [Conceptual Data Model](#1-conceptual-data-model)
2. [Logical Data Model](#2-logical-data-model)
3. [Entity Relationship Diagram (ERD)](#3-entity-relationship-diagram-erd)
4. [Data Dictionary](#4-data-dictionary)

---

## 1. Conceptual Data Model

### 1.1 Business Entities and Relationships

The Banking Core Platform is decomposed into four bounded contexts, each owning its data model. The following describes the key business entities and their conceptual relationships.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CONCEPTUAL DATA MODEL                               │
│                         Banking Core Platform                               │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────┐         AUTHENTICATES         ┌────────────────────┐
  │   Customer  │◄─────────────────────────────►│  UserCredential    │
  │  (real-world│  1                          1  │  (auth_db.users)   │
  │   person)   │                                └──────────┬─────────┘
  └──────┬──────┘                                           │ 1
         │ 1                                                │
         │                                        ┌─────────▼─────────┐
         │ HAS                                    │   RefreshToken     │
         │                                        │ (rotated session)  │
  ┌──────▼──────┐                                └────────────────────┘
  │  UserProfile│
  │(user_db.    │   1                      1   ┌────────────────────┐
  │ users_prof) │◄────────────────────────────►│     Account        │
  └─────────────┘   HOLDS                      │ (account_db.       │
                                                │  accounts)         │
                                                └──────────┬─────────┘
                                                           │ 1
                                                           │ PARTICIPATES IN
                                                      (N)  │
                                              ┌────────────▼────────────┐
                                              │      Transaction         │
                                              │ (transaction_db.        │
                                              │  transactions)          │
                                              │ fromAccount → Account   │
                                              │ toAccount   → Account   │
                                              └─────────────────────────┘


  Supporting Entities (within auth_db):
  ┌──────────────────────────────────┐
  │  EmailVerificationToken          │
  │  PasswordResetToken              │
  └──────────────────────────────────┘

  Cross-cutting:
  ┌──────────────────────────────────┐
  │  AuditLog  (future service)      │
  │  Notification (email events)     │
  └──────────────────────────────────┘
```

### 1.2 Business Entity Descriptions

| Entity | Business Meaning | Service Owner |
|--------|-----------------|---------------|
| **UserCredential** | Stores authentication credentials for a registered bank user. Contains hashed password, roles, and account activation flags. This is the security identity of the customer. | auth-service (auth_db) |
| **RefreshToken** | Represents a long-lived session token that enables JWT refresh without re-login. Stored as a SHA-256 hash. Supports rotation with theft detection. | auth-service (auth_db) |
| **EmailVerificationToken** | Single-use, time-limited token sent to a customer's email address to verify ownership before account activation. | auth-service (auth_db) |
| **PasswordResetToken** | Single-use, time-limited token allowing a customer to reset their password via a secure email link. | auth-service (auth_db) |
| **UserProfile (CIF)** | The Customer Information File — stores personal details about the bank customer: name, phone, avatar, KYC status. This is the customer's identity within the banking platform. | user-service (user_db) |
| **Account (CASA)** | Represents a customer's bank account (Current Account). Holds the account number, balance, currency, and operational status. The financial instrument owned by the customer. | account-service (account_db) |
| **Transaction** | Records a fund transfer event between two accounts. Maintains a lifecycle state (PENDING → SUCCESS/FAILED) and idempotency key to prevent duplicate processing. | transaction-service (transaction_db) |
| **Beneficiary** | (Phase 2) A saved payee record storing destination account details for recurring transfers. Linked to a customer CIF. | beneficiary-service (future) |
| **AuditLog** | (Phase 2) Immutable record of all significant actions performed on the platform by customers or bank staff. | audit-service (future) |
| **Notification** | Represents an outbound communication event (email) triggered by platform actions (registration, transfer, account freeze). | notification-service (via SMTP) |

---

## 2. Logical Data Model

### 2.1 auth-service — auth_db

#### Table: users

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | VARCHAR(36) | PK, NOT NULL | UUID string — user's unique identity; used as JWT `sub` claim |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Login username; immutable after creation |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Customer email address; used for notifications and verification |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt-hashed password (work factor 10); plaintext never stored |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'ROLE_USER' | Authorisation role: ROLE_USER or ROLE_ADMIN |
| enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | Account activation flag; false until email verified |
| email_verified | BOOLEAN | NOT NULL, DEFAULT FALSE | Email verification status; set true on token validation |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Account creation timestamp (UTC) |

#### Table: refresh_tokens

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incremented surrogate key |
| user_id | VARCHAR(36) | FK → users.id, NOT NULL | Owning user; many tokens per user supported |
| token_hash | VARCHAR(64) | NOT NULL | SHA-256 hash of the raw refresh token; raw value never persisted |
| expires_at | TIMESTAMP | NOT NULL | Token expiry datetime (UTC); default NOW + 7 days |
| revoked | BOOLEAN | NOT NULL, DEFAULT FALSE | Revocation flag; true after rotation or theft detection |
| replaced_by_token | VARCHAR(64) | NULLABLE | Hash of the successor token in rotation chain; enables audit trail |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Token issuance timestamp (UTC) |

#### Table: email_verification_tokens

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incremented surrogate key |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Random UUID token included in the verification email link |
| user_id | VARCHAR(36) | FK → users.id, NOT NULL | Associated user account |
| expires_at | TIMESTAMP | NOT NULL | Token expiry datetime; default NOW + 24 hours |
| used | BOOLEAN | NOT NULL, DEFAULT FALSE | Prevents token reuse after first consumption |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Token creation timestamp (UTC) |

#### Table: password_reset_tokens

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incremented surrogate key |
| user_id | VARCHAR(36) | NOT NULL | Associated user (no FK to support deletion edge cases) |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Random UUID token included in the password reset email link |
| expires_at | TIMESTAMP | NOT NULL | Token expiry datetime; default NOW + 1 hour |
| used | BOOLEAN | NOT NULL, DEFAULT FALSE | Prevents token reuse |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Token creation timestamp (UTC) |

---

### 2.2 user-service — user_db

#### Table: users_profile

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | UUID | PK, NOT NULL | Profile UUID — randomly generated at profile creation |
| auth_id | VARCHAR(36) | UNIQUE, NOT NULL | Links to auth-service `users.id` (JWT sub claim); enables cross-service identity resolution |
| email | VARCHAR(255) | NOT NULL | Customer email address (copied from registration event) |
| username | VARCHAR(50) | NULLABLE | Customer's chosen username (copied from UserCreatedEvent) |
| full_name | VARCHAR(100) | NULLABLE | Customer's full legal name (updated during KYC) |
| phone | VARCHAR(20) | NULLABLE | Customer's mobile phone number |
| avatar | VARCHAR(500) | NULLABLE | URL or relative path to customer's profile image |
| kyc_status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | KYC verification status: PENDING / IN_REVIEW / VERIFIED / REJECTED |
| created_at | TIMESTAMP | NOT NULL | Profile creation timestamp (UTC); set on INSERT |
| updated_at | TIMESTAMP | NOT NULL | Last profile update timestamp (UTC); auto-updated on any change |

---

### 2.3 account-service — account_db

#### Table: accounts

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | UUID | PK, NOT NULL | Account UUID — randomly generated at account creation |
| user_id | VARCHAR(36) | NOT NULL, INDEX | Owning customer's auth-service user ID; not a FK (microservice boundary) |
| account_number | VARCHAR(12) | UNIQUE, NOT NULL, INDEX | 10-digit zero-padded numeric account number; assigned at account creation |
| balance | NUMERIC(19,2) | NOT NULL, DEFAULT 0.00 | Current ledger balance; NUMERIC(19,2) mandatory for monetary precision |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'VND' | ISO 4217 currency code; VND for Phase 1 |
| status | VARCHAR(20) | NOT NULL | Account operational status: ACTIVE / SUSPENDED / CLOSED |
| version | BIGINT | NOT NULL, DEFAULT 0 | JPA optimistic lock counter; incremented on every balance update |
| created_at | TIMESTAMP | NOT NULL | Account creation timestamp (UTC) |
| updated_at | TIMESTAMP | NOT NULL | Last account modification timestamp (UTC) |

**Indexes:**
- `idx_accounts_user_id` on `user_id` (for lookup by user)
- `idx_accounts_account_number` on `account_number` UNIQUE (for fast transfer validation)

---

### 2.4 transaction-service — transaction_db

#### Table: transactions

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| id | UUID | PK, NOT NULL | Transaction UUID — randomly generated for each transfer attempt |
| idempotency_key | VARCHAR(64) | UNIQUE, NULLABLE | Client-supplied idempotency key; unique DB constraint prevents duplicate processing |
| from_account | VARCHAR(12) | NOT NULL, INDEX | Source account number (debit account) |
| to_account | VARCHAR(12) | NOT NULL, INDEX | Destination account number (credit account) |
| amount | NUMERIC(19,2) | NOT NULL | Transfer amount; scaled to 2 decimal places (HALF_UP rounding) |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'VND' | ISO 4217 currency code; derived from source account currency |
| status | VARCHAR(10) | NOT NULL, DEFAULT 'PENDING' | Transaction lifecycle: PENDING / SUCCESS / FAILED |
| failure_reason | TEXT | NULLABLE | Human-readable description of failure; populated only when status = FAILED |
| created_at | TIMESTAMP | NOT NULL | Transaction initiation timestamp (UTC); immutable after creation |
| updated_at | TIMESTAMP | NOT NULL | Last status update timestamp (UTC) |

**Indexes:**
- `idx_tx_from_account` on `from_account` (for history queries)
- `idx_tx_to_account` on `to_account` (for history queries)
- `idx_tx_idempotency` on `idempotency_key` UNIQUE (for idempotency check)

---

## 3. Entity Relationship Diagram (ERD)

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                    BANKING CORE PLATFORM — ERD (Logical)                   ║
║                    (Cross-service boundaries shown as dotted lines)         ║
╚══════════════════════════════════════════════════════════════════════════════╝

 ┌──────────────────────────── AUTH_DB ─────────────────────────────────┐
 │                                                                       │
 │  ┌──────────────────────┐         ┌───────────────────────────┐      │
 │  │       users          │  1    N │    refresh_tokens          │      │
 │  │──────────────────────│◄────────│───────────────────────────│      │
 │  │ PK id (VARCHAR 36)   │         │ PK id (BIGSERIAL)         │      │
 │  │    username (UNIQUE) │         │ FK user_id → users.id     │      │
 │  │    email (UNIQUE)    │         │    token_hash (SHA-256)   │      │
 │  │    password_hash     │         │    expires_at             │      │
 │  │    role              │         │    revoked (BOOLEAN)      │      │
 │  │    enabled           │         │    replaced_by_token      │      │
 │  │    email_verified    │         └───────────────────────────┘      │
 │  │    created_at        │                                             │
 │  └──────────┬───────────┘         ┌───────────────────────────┐      │
 │             │ 1                 N │ email_verification_tokens  │      │
 │             └────────────────────►│───────────────────────────│      │
 │                                   │ PK id (BIGSERIAL)         │      │
 │                                   │ FK user_id → users.id     │      │
 │                                   │    token (UNIQUE)         │      │
 │                                   │    expires_at             │      │
 │                                   │    used (BOOLEAN)         │      │
 │                                   └───────────────────────────┘      │
 │                                                                       │
 │                                   ┌───────────────────────────┐      │
 │                                   │   password_reset_tokens    │      │
 │                                   │───────────────────────────│      │
 │                                   │ PK id (BIGSERIAL)         │      │
 │                                   │    user_id (VARCHAR)      │      │
 │                                   │    token (UNIQUE)         │      │
 │                                   │    expires_at             │      │
 │                                   │    used (BOOLEAN)         │      │
 │                                   └───────────────────────────┘      │
 └───────────────────────────────────────────────────────────────────────┘

                    │ UserCreatedEvent (Kafka)
                    │ auth_id = users.id
                    ▼ (logical, not FK)

 ┌──────────────────────── USER_DB ─────────────────────────────────────┐
 │                                                                       │
 │  ┌────────────────────────────────────────────────┐                  │
 │  │                  users_profile                  │                  │
 │  │────────────────────────────────────────────────│                  │
 │  │ PK id (UUID)                                   │                  │
 │  │    auth_id (VARCHAR 36) UNIQUE ←── from JWT sub│                  │
 │  │    email (VARCHAR 255)                         │                  │
 │  │    username (VARCHAR 50)                       │                  │
 │  │    full_name                                   │                  │
 │  │    phone                                       │                  │
 │  │    avatar                                      │                  │
 │  │    kyc_status: PENDING/IN_REVIEW/VERIFIED/     │                  │
 │  │                REJECTED                        │                  │
 │  │    created_at, updated_at                      │                  │
 │  └────────────────────────────────────────────────┘                  │
 └───────────────────────────────────────────────────────────────────────┘

                    │ UserCreatedEvent (Kafka)
                    │ user_id = users.id
                    ▼ (logical, not FK)

 ┌──────────────────────── ACCOUNT_DB ──────────────────────────────────┐
 │                                                                       │
 │  ┌────────────────────────────────────────────────┐                  │
 │  │                    accounts                     │                  │
 │  │────────────────────────────────────────────────│                  │
 │  │ PK id (UUID)                                   │                  │
 │  │    user_id (VARCHAR 36) ← no FK; microservice  │                  │
 │  │    account_number (VARCHAR 12) UNIQUE           │                  │
 │  │    balance (NUMERIC 19,2) ≥ 0                  │                  │
 │  │    currency (VARCHAR 3) DEFAULT 'VND'          │                  │
 │  │    status: ACTIVE/SUSPENDED/CLOSED             │                  │
 │  │    version (BIGINT) optimistic lock            │                  │
 │  │    created_at, updated_at                      │                  │
 │  └────────────────────────────────────────────────┘                  │
 └───────────────────────────────────────────────────────────────────────┘

                    │ REST: account_number used in transactions
                    ▼ (logical reference, not FK)

 ┌──────────────────────── TRANSACTION_DB ──────────────────────────────┐
 │                                                                       │
 │  ┌────────────────────────────────────────────────┐                  │
 │  │                  transactions                   │                  │
 │  │────────────────────────────────────────────────│                  │
 │  │ PK id (UUID)                                   │                  │
 │  │    idempotency_key (VARCHAR 64) UNIQUE         │                  │
 │  │    from_account (VARCHAR 12) ─────► accounts   │                  │
 │  │    to_account   (VARCHAR 12) ─────► accounts   │                  │
 │  │    amount (NUMERIC 19,2)                       │                  │
 │  │    currency (VARCHAR 3)                        │                  │
 │  │    status: PENDING/SUCCESS/FAILED              │                  │
 │  │    failure_reason (TEXT)                       │                  │
 │  │    created_at, updated_at                      │                  │
 │  └────────────────────────────────────────────────┘                  │
 └───────────────────────────────────────────────────────────────────────┘
```

### 3.1 ERD Relationship Legend

| Notation | Meaning |
|----------|---------|
| `1 — N` | One-to-many relationship with FK enforced in DB |
| `→ (logical)` | Cross-service reference by shared key value; no DB-level FK |
| `UNIQUE` | Unique constraint enforced at DB level |
| `PK` | Primary Key |
| `FK` | Foreign Key |

---

## 4. Data Dictionary

### 4.1 auth_db.users

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | VARCHAR | 36 | No | Yes | Unique user identifier in UUID string format. Used as the JWT `sub` (subject) claim across all services. The cross-service identity anchor. |
| username | VARCHAR | 50 | No | Yes | The customer's chosen login name. Must be globally unique. Alphanumeric; no spaces. Immutable after registration. Used in JWT `username` claim. |
| email | VARCHAR | 255 | No | Yes | Customer's primary email address. Used for verification, password reset, and notifications. Must be globally unique. |
| password_hash | VARCHAR | 255 | No | Yes | BCrypt-hashed version of the customer's password (work factor 10). The plaintext password is never stored or logged. |
| role | VARCHAR | 20 | No | Yes | Authorisation role assigned to the user. Valid values: `ROLE_USER` (default) or `ROLE_ADMIN`. Embedded in JWT roles claim. |
| enabled | BOOLEAN | — | No | Yes | Determines whether the account can be used for authentication. Set to `true` only after email verification is complete. |
| email_verified | BOOLEAN | — | No | Yes | Tracks whether the customer has clicked the email verification link. Must be `true` for login to succeed. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp when the user record was first created (i.e., when the customer submitted the registration form). |

---

### 4.2 auth_db.refresh_tokens

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | BIGSERIAL | — | No | Yes | Auto-incremented surrogate primary key. No business meaning. |
| user_id | VARCHAR | 36 | No | Yes | Foreign key to users.id. Identifies which user this refresh token belongs to. One user may have multiple active refresh tokens (e.g., different devices). |
| token_hash | VARCHAR | 64 | No | Yes | SHA-256 hash of the raw refresh token. The raw token is returned to the client once and never stored. Enables server-side validation without storing sensitive material. |
| expires_at | TIMESTAMP | — | No | Yes | Expiry datetime. Tokens submitted after this timestamp are rejected. Default: 7 days from issuance. |
| revoked | BOOLEAN | — | No | Yes | Revocation flag. Set to `true` when the token is rotated (replaced) or when theft detection is triggered. Revoked tokens cannot be used. |
| replaced_by_token | VARCHAR | 64 | Yes | No | Hash of the token that replaced this one during rotation. Creates an auditable rotation chain. NULL for currently active tokens. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp of token issuance (login or previous refresh). |

---

### 4.3 auth_db.email_verification_tokens

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | BIGSERIAL | — | No | Yes | Auto-incremented surrogate primary key. |
| token | VARCHAR | 255 | No | Yes | Random UUID string included in the verification email link as a query parameter. Single-use; unique DB constraint prevents duplicate tokens. |
| user_id | VARCHAR | 36 | No | Yes | FK to users.id. The user whose email address this token is verifying. |
| expires_at | TIMESTAMP | — | No | Yes | Token expiry datetime (UTC). Default: 24 hours from creation. Expired tokens are rejected; a new token must be requested. |
| used | BOOLEAN | — | No | Yes | Marks the token as consumed. Once `true`, the token cannot activate an account again even if the link is replayed. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp when the token was generated and email dispatched. |

---

### 4.4 auth_db.password_reset_tokens

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | BIGSERIAL | — | No | Yes | Auto-incremented surrogate primary key. |
| user_id | VARCHAR | 36 | No | Yes | Identifies the user who requested the password reset. Stored as plain VARCHAR (no FK) to support edge cases where user may be in a partially-created state. |
| token | VARCHAR | 255 | No | Yes | Random UUID string sent in the password reset email. Unique DB constraint. Single-use. |
| expires_at | TIMESTAMP | — | No | Yes | Token expiry datetime (UTC). Default: 1 hour from creation. Shorter than verification token due to higher security sensitivity. |
| used | BOOLEAN | — | No | Yes | Marks token as consumed after the password is successfully reset. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp when the reset was requested. |

---

### 4.5 user_db.users_profile

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | UUID | — | No | Yes | Profile UUID, randomly generated at profile creation. The primary key within user-service. Distinct from auth_id. |
| auth_id | VARCHAR | 36 | No | Yes | The auth-service user ID (users.id). This is the cross-service identity link — when user-service receives a JWT, it maps the JWT `sub` claim to auth_id to find the profile. Unique constraint enforces one profile per auth identity. |
| email | VARCHAR | 255 | No | Yes | Customer email copied from the UserCreatedEvent. Used for display and notifications within user-service. |
| username | VARCHAR | 50 | Yes | No | Customer's username, copied from the UserCreatedEvent. Nullable as some OAuth2 flows may not provide a username immediately. |
| full_name | VARCHAR | 100 | Yes | No | Customer's full legal name. Collected during profile update or KYC process. Required for KYC completion. |
| phone | VARCHAR | 20 | Yes | No | Customer's mobile phone number. Format: E.164 standard (e.g., +84912345678). Required for account operations alerts in Phase 2. |
| avatar | VARCHAR | 500 | Yes | No | URL or relative file path to the customer's profile photo. Updated by the customer via self-service. |
| kyc_status | VARCHAR | 20 | No | Yes | Reflects the customer's KYC verification status. Valid values: `PENDING` (default), `IN_REVIEW`, `VERIFIED`, `REJECTED`. Controls transaction limits and access to premium features. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp when the profile was first created (triggered by Kafka event). |
| updated_at | TIMESTAMP | — | No | Yes | UTC timestamp of the most recent profile update. Auto-managed by Hibernate `@UpdateTimestamp`. |

---

### 4.6 account_db.accounts

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | UUID | — | No | Yes | Account UUID, randomly generated at account creation. Primary key within account-service. |
| user_id | VARCHAR | 36 | No | Yes | The owning customer's auth-service user ID. Stored as a plain VARCHAR (no FK — microservice boundary). Used to retrieve the account when a user initiates a transfer. |
| account_number | VARCHAR | 12 | No | Yes | The 10-digit numeric account number displayed to customers and used to identify accounts in transfers. Generated by collision-resistant random algorithm. Unique DB constraint. |
| balance | NUMERIC | 19,2 | No | Yes | The current ledger balance of the account. NUMERIC(19,2) type is mandatory for monetary values — never float or double. Scale 2 enforces cent-level precision. Must never go negative (enforced in service logic). |
| currency | VARCHAR | 3 | No | Yes | ISO 4217 currency code. `VND` for Phase 1. Future phases may support `USD`, `EUR`. Determines rounding rules for monetary operations. |
| status | VARCHAR | 20 | No | Yes | Account operational status. `ACTIVE`: account can transact normally. `SUSPENDED`: debits blocked, pending investigation. `CLOSED`: permanently deactivated; no transactions permitted. |
| version | BIGINT | — | No | Yes | JPA `@Version` field. Incremented by Hibernate on every UPDATE. Provides optimistic locking protection — a stale read throws `OptimisticLockException` rather than silently overwriting. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp of account creation (triggered by Kafka event). |
| updated_at | TIMESTAMP | — | No | Yes | UTC timestamp of the most recent balance or status change. Auto-managed by Hibernate `@UpdateTimestamp`. |

---

### 4.7 transaction_db.transactions

| Field Name | Data Type | Length | Nullable | Mandatory | Business Meaning |
|------------|-----------|--------|----------|-----------|-----------------|
| id | UUID | — | No | Yes | Transaction UUID, randomly generated for each transfer attempt. Serves as the receipt reference for the customer. |
| idempotency_key | VARCHAR | 64 | Yes | No | Client-supplied unique key for duplicate prevention. If a client retries a transfer request with the same key, the original transaction is returned without re-processing. Unique DB constraint ensures atomic idempotency check-and-create. Nullable — if not supplied, all retries are treated as new transfers. |
| from_account | VARCHAR | 12 | No | Yes | Account number of the transfer source (debit account). Logically references account_db.accounts.account_number but no cross-DB FK. Indexed for transaction history queries. |
| to_account | VARCHAR | 12 | No | Yes | Account number of the transfer destination (credit account). Logically references account_db.accounts.account_number. Indexed for transaction history queries. |
| amount | NUMERIC | 19,2 | No | Yes | The monetary value transferred between accounts. Must be greater than zero. Stored at 2 decimal places (HALF_UP rounding applied). VND does not use subunit in practice, but precision is maintained for multi-currency future-proofing. |
| currency | VARCHAR | 3 | No | Yes | ISO 4217 currency code of the transaction. Derived from the source account's currency at time of transfer. |
| status | VARCHAR | 10 | No | Yes | Transaction lifecycle state. `PENDING`: recorded but not yet executed. `SUCCESS`: both debit and credit completed. `FAILED`: one or both operations failed; balances may need reconciliation. |
| failure_reason | TEXT | — | Yes | No | Human-readable explanation of why the transaction failed. Populated only when status = `FAILED`. Examples: "Insufficient balance", "Account is not ACTIVE", "Account service unavailable". Never null when status = FAILED. |
| created_at | TIMESTAMP | — | No | Yes | UTC timestamp when the transaction record was first created (PENDING state). Immutable — never updated. |
| updated_at | TIMESTAMP | — | No | Yes | UTC timestamp of the most recent status update. Changes from PENDING to SUCCESS or FAILED. |

---

### 4.8 Kafka Event: UserCreatedEvent

| Field Name | Data Type | Nullable | Business Meaning |
|------------|-----------|----------|-----------------|
| id | String | No | The auth-service user ID (users.id). Consumers use this as the cross-service identity reference. In account-service this becomes user_id; in user-service this becomes auth_id. |
| email | String | No | The customer's registered email address. Used by user-service to populate the profile. |
| username | String | No | The customer's chosen username. Used by user-service to populate the profile. |

**Topic:** `user-events`  
**Key:** `event.getId()` (userId string) — ensures ordered delivery per user  
**Serialisation:** JSON (spring-kafka with JsonSerializer)  
**Consumers:** user-service-group, account-service-group  
**Delivery guarantee:** At-least-once (consumers must be idempotent)

---

*Document End — BCP-DM-001 v1.0*
