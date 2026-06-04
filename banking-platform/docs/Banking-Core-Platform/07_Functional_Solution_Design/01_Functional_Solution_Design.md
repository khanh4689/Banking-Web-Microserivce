# Banking Core Platform
## Document 07 — Functional Solution Design

**Document ID:** BCP-FSD-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Solution Architecture / Business Analysis Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [FSD-001 Customer Registration Module](#fsd-001-customer-registration-module)
2. [FSD-002 eKYC Module](#fsd-002-ekyc-module)
3. [FSD-003 Authentication & Login Module](#fsd-003-authentication--login-module)
4. [FSD-004 Account Management Module](#fsd-004-account-management-module)
5. [FSD-005 Fund Transfer Module](#fsd-005-fund-transfer-module)
6. [FSD-006 Beneficiary Management Module](#fsd-006-beneficiary-management-module)
7. [FSD-007 Transaction History Module](#fsd-007-transaction-history-module)

---

## FSD-001 Customer Registration Module

### 1.1 Functional Description

The Customer Registration module enables prospective customers to self-enrol on the Banking Core Platform without requiring in-person branch visits. The module spans the auth-service and triggers downstream provisioning of a user profile (user-service) and bank account (account-service) via the Kafka event bus.

The registration process enforces credential uniqueness, password strength, email verification, and data quality at every step. Account activation is conditional on email verification to prevent registration abuse and satisfy regulatory identity confirmation requirements.

### 1.2 Service Owner
`auth-service` (Port 8082)

### 1.3 Inputs

| Field | Type | Mandatory | Validation |
|-------|------|-----------|------------|
| username | String | Yes | 3–50 chars; alphanumeric only; globally unique |
| email | String | Yes | Valid RFC 5322 format; globally unique; max 255 chars |
| password | String | Yes | Min 8 chars; must contain uppercase, lowercase, and digit; never logged or stored in plaintext |

### 1.4 Outputs

| Scenario | HTTP Status | Response Body |
|----------|-------------|---------------|
| Registration successful | 200 OK | `{ "message": "User registered successfully. Please verify your email." }` |
| Username conflict | 409 Conflict | `{ "error": "Username already taken." }` |
| Email conflict | 409 Conflict | `{ "error": "Email already registered." }` |
| Validation failure | 400 Bad Request | `{ "errors": [{ "field": "password", "message": "..." }] }` |

### 1.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-REG-001 | One account per unique email address. Duplicate email registrations are rejected. |
| BR-REG-002 | One account per unique username. Duplicate usernames are rejected. |
| BR-REG-003 | Account is created in a disabled state (enabled=false, emailVerified=false) until email is verified. |
| BR-REG-004 | A BCrypt-hashed copy of the password is stored. The plaintext password is never written to disk, database, or log. |
| BR-REG-005 | The default role assigned to all new registrations is ROLE_USER. |
| BR-REG-006 | A UserCreatedEvent is published to Kafka ONLY after the registration database transaction commits (via @TransactionalEventListener AFTER_COMMIT). This prevents downstream consumers from receiving events for uncommitted (ghost) users. |

### 1.6 Validation Rules

| Rule ID | Field | Rule | Error Code | Error Message |
|---------|-------|------|------------|---------------|
| VAL-REG-001 | username | Required | FIELD_REQUIRED | "Username is required." |
| VAL-REG-002 | username | Min 3, max 50 chars | FIELD_LENGTH | "Username must be 3–50 characters." |
| VAL-REG-003 | username | Alphanumeric only | FIELD_FORMAT | "Username can only contain letters and numbers." |
| VAL-REG-004 | username | Unique | DUPLICATE_USERNAME | "Username already taken." |
| VAL-REG-005 | email | Required | FIELD_REQUIRED | "Email is required." |
| VAL-REG-006 | email | Valid email format | FIELD_FORMAT | "Invalid email address." |
| VAL-REG-007 | email | Unique | DUPLICATE_EMAIL | "Email already registered." |
| VAL-REG-008 | password | Required | FIELD_REQUIRED | "Password is required." |
| VAL-REG-009 | password | Min 8 chars | FIELD_LENGTH | "Password must be at least 8 characters." |
| VAL-REG-010 | password | Must include uppercase | PASSWORD_WEAK | "Password must include at least one uppercase letter." |
| VAL-REG-011 | password | Must include lowercase | PASSWORD_WEAK | "Password must include at least one lowercase letter." |
| VAL-REG-012 | password | Must include digit | PASSWORD_WEAK | "Password must include at least one number." |

### 1.7 Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| FIELD_REQUIRED | 400 | A mandatory field is missing |
| FIELD_LENGTH | 400 | Field value violates length constraint |
| FIELD_FORMAT | 400 | Field value fails format validation (email, alphanumeric) |
| PASSWORD_WEAK | 400 | Password does not meet complexity requirements |
| DUPLICATE_USERNAME | 409 | Username already exists in the system |
| DUPLICATE_EMAIL | 409 | Email already registered |
| INTERNAL_ERROR | 500 | Unexpected server-side error |

### 1.8 Integration Requirements

| Integration | Type | Direction | Description |
|-------------|------|-----------|-------------|
| SMTP Email Service | External | Outbound | Send verification email with token link |
| Kafka (user-events) | Message Bus | Outbound | Publish UserCreatedEvent after commit |
| user-service | Kafka Consumer | Downstream | Consumes UserCreatedEvent to create UserProfile |
| account-service | Kafka Consumer | Downstream | Consumes UserCreatedEvent to create Account |

### 1.9 API Specification

**POST /auth/register**

```
Request:
  Content-Type: application/json
  Body: {
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass1"
  }

Response (200):
  {
    "success": true,
    "message": "User registered successfully. Please verify your email.",
    "data": null
  }

Response (409):
  {
    "success": false,
    "errorCode": "DUPLICATE_USERNAME",
    "message": "Username already taken.",
    "data": null
  }
```

**GET /auth/verify-email?token={token}**

```
Response (200):
  { "success": true, "message": "Email verified successfully." }

Response (400):
  { "success": false, "message": "Invalid or expired verification token." }
```

### 1.10 Audit Requirements

| Event | Logged Data | Log Level |
|-------|-------------|-----------|
| Registration attempt | username (no password), email, traceId, timestamp | INFO |
| Registration success | userId, username, email, traceId | INFO |
| Registration failure (duplicate) | username/email (whichever duplicated), error code | WARN |
| Email verification success | userId, email, tokenExpiry, traceId | INFO |
| Email verification failure | tokenId, reason (expired/used/invalid), traceId | WARN |
| UserCreatedEvent published | userId, email, traceId | INFO |

### 1.11 Security Requirements

- Password must be hashed with BCrypt before persistence; plaintext password must be cleared from memory immediately after hashing.
- Verification tokens must be cryptographically random UUIDs.
- Registration endpoint must not be rate-limited in Phase 1 but should have bot protection (CAPTCHA) considered for Phase 2.
- The response to duplicate email/username must be generic enough to prevent user enumeration in publicly accessible environments (consider Phase 2 enhancement).

---

## FSD-002 eKYC Module

### 2.1 Functional Description

The eKYC module manages the Know Your Customer verification lifecycle for each customer. In Phase 1, KYC status management is performed by authorised bank staff (Operations Officers, Compliance Officers) via admin APIs. Phase 2 will integrate with an external eKYC vendor for automated document and biometric verification.

### 2.2 Service Owner
`user-service` (Port 8081)

### 2.3 Inputs

| Operation | Input Fields | Source |
|-----------|-------------|--------|
| Submit KYC documents | fullName, dateOfBirth, nationality, idType, idNumber, documentImages (front, back, selfie) | Customer via PUT /users/me |
| Update KYC status | userId, newStatus, reason, notes, referenceNumber | Bank staff via PATCH /users/{id}/kyc |

### 2.4 Outputs

| Scenario | HTTP Status | Response |
|----------|-------------|----------|
| KYC update success | 200 OK | Updated UserProfile with new kycStatus |
| User not found | 404 | `{ "error": "User not found" }` |
| Unauthorised status change | 403 | `{ "error": "Access denied" }` |
| Invalid status transition | 400 | `{ "error": "Invalid status transition" }` |

### 2.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-KYC-001 | KYC status initial value is PENDING for all new registrations. |
| BR-KYC-002 | Valid KYC status values: PENDING, IN_REVIEW, VERIFIED, REJECTED. |
| BR-KYC-003 | Valid transitions: PENDING → IN_REVIEW, IN_REVIEW → VERIFIED, IN_REVIEW → REJECTED, REJECTED → PENDING (re-submission). |
| BR-KYC-004 | Only users with ROLE_OPERATIONS or ROLE_COMPLIANCE can change kycStatus. Customers cannot self-approve their own KYC. |
| BR-KYC-005 | Every KYC status change must create an audit log entry capturing actor, old status, new status, reason, and timestamp. |
| BR-KYC-006 | Customers with kycStatus = PENDING or REJECTED are subject to reduced transaction limits (Phase 2 enforcement). |
| BR-KYC-007 | KYC records must be retained for minimum 5 years after account closure per SBV AML Circular. |

### 2.6 Validation Rules

| Rule ID | Field | Rule |
|---------|-------|------|
| VAL-KYC-001 | newStatus | Must be one of PENDING, IN_REVIEW, VERIFIED, REJECTED |
| VAL-KYC-002 | newStatus | Transition must follow the permitted state machine |
| VAL-KYC-003 | reason | Required when newStatus = REJECTED |
| VAL-KYC-004 | referenceNumber | Required when reason = AML_INVESTIGATION or COURT_ORDER |
| VAL-KYC-005 | idNumber | Format validation based on idType: National ID = 12 digits; Passport = 8 alphanumeric chars |

### 2.7 Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| INVALID_STATUS_TRANSITION | 400 | Attempted transition not in permitted state machine |
| REASON_REQUIRED | 400 | Rejection reason missing |
| KYC_ACCESS_DENIED | 403 | User lacks ROLE_OPERATIONS or ROLE_COMPLIANCE |
| USER_NOT_FOUND | 404 | Target user profile does not exist |

### 2.8 API Specification

**PATCH /users/{id}/kyc** (Authorised Staff Only)

```
Request:
  Authorization: Bearer {adminJWT}
  Content-Type: application/json
  Body: {
    "status": "VERIFIED",
    "reason": null,
    "notes": "All documents valid. Verified via eKYC.",
    "referenceNumber": "KYC-2026-00123"
  }

Response (200):
  {
    "id": "uuid",
    "authId": "auth-uuid",
    "email": "john@example.com",
    "kycStatus": "VERIFIED",
    "updatedAt": "2026-06-04T10:30:00Z"
  }
```

### 2.9 Audit Requirements

| Event | Logged Data |
|-------|-------------|
| KYC status updated | actorId, targetUserId, oldStatus, newStatus, reason, referenceNumber, timestamp |
| KYC update unauthorised | actorId, targetUserId, attemptedAction, timestamp |

### 2.10 Security Requirements

- Only ROLE_OPERATIONS and ROLE_COMPLIANCE may update kycStatus.
- Customers may submit documents (put into IN_REVIEW) but cannot approve themselves.
- KYC documents must be stored in encrypted object storage.
- Document access must be logged.

---

## FSD-003 Authentication & Login Module

### 3.1 Functional Description

The Authentication module handles customer identity verification at login, issues JWT access tokens signed with RSA RS256, manages refresh token rotation, and provides password reset functionality. All resource services validate incoming JWTs against the JWKS public key endpoint exposed by auth-service.

### 3.2 Service Owner
`auth-service` (Port 8082)

### 3.3 Inputs

| Operation | Input | Validation |
|-----------|-------|------------|
| Login | username (String), password (String) | Both required; username exists; password matches BCrypt hash |
| Token Refresh | refreshToken (String) | Required; valid SHA-256 hash found in DB; not revoked; not expired |
| Forgot Password | email (String) | Required; valid format |
| Reset Password | token (String), newPassword (String) | Token exists, unexpired, unused; password meets complexity |

### 3.4 Outputs

| Scenario | HTTP Status | Response |
|----------|-------------|----------|
| Login success | 200 OK | `{ accessToken, refreshToken, expiresIn }` |
| Login failure (bad creds) | 401 | `{ "error": "Invalid credentials" }` |
| Login failure (email not verified) | 401 | `{ "error": "Email not verified" }` |
| Token refresh success | 200 OK | `{ accessToken, refreshToken, expiresIn }` |
| Token refresh failure | 401 | `{ "error": "Invalid or expired refresh token" }` |

### 3.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-AUTH-001 | Passwords are stored as BCrypt hashes (work factor 10). Plaintext passwords are never persisted. |
| BR-AUTH-002 | JWT access tokens are signed with RSA 2048-bit private key; expire after 10 minutes. |
| BR-AUTH-003 | JWT claims: sub (userId UUID), username, roles ([]), iat, exp. |
| BR-AUTH-004 | Refresh tokens are stored as SHA-256 hashes only. Raw token is returned to client once and never stored. |
| BR-AUTH-005 | Refresh token lifetime: 7 days. |
| BR-AUTH-006 | On token rotation: old token is revoked, replaced_by_token is set to new token hash. |
| BR-AUTH-007 | If a revoked refresh token is submitted: all tokens for the user are immediately revoked (theft detection). |
| BR-AUTH-008 | Password reset tokens expire after 1 hour. Single-use only. |
| BR-AUTH-009 | Forgot password response is always HTTP 200 regardless of whether the email exists (prevents user enumeration). |
| BR-AUTH-010 | Login failure must not reveal whether the username exists (generic "Invalid credentials" message). |

### 3.6 Validation Rules

| Rule ID | Field | Rule | Error |
|---------|-------|------|-------|
| VAL-AUTH-001 | username | Required, non-empty | "Username is required" |
| VAL-AUTH-002 | password | Required, non-empty | "Password is required" |
| VAL-AUTH-003 | refreshToken | Required, non-empty | "Refresh token is required" |
| VAL-AUTH-004 | newPassword | Min 8 chars, includes uppercase, lowercase, digit | "Password does not meet complexity requirements" |

### 3.7 Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| INVALID_CREDENTIALS | 401 | Username not found or password mismatch |
| EMAIL_NOT_VERIFIED | 401 | User exists but emailVerified = false |
| ACCOUNT_DISABLED | 401 | User's enabled flag = false |
| TOKEN_EXPIRED | 401 | Refresh token has passed its expiry date |
| TOKEN_REVOKED | 401 | Refresh token has been revoked (rotation or theft) |
| TOKEN_NOT_FOUND | 401 | Submitted refresh token hash not found in DB |
| SESSION_TERMINATED | 401 | All user tokens revoked due to theft detection |

### 3.8 JWT Token Structure

```json
Header:
{
  "alg": "RS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "roles": ["ROLE_USER"],
  "iat": 1749030000,
  "exp": 1749030600
}
```

### 3.9 JWKS Endpoint

**GET /.well-known/jwks.json** (Public, No Auth)

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "banking-platform-key-1",
      "n": "{base64url-encoded-modulus}",
      "e": "AQAB"
    }
  ]
}
```

All resource services (user-service, account-service, transaction-service) fetch this endpoint at startup and cache the public key for JWT validation.

### 3.10 Audit Requirements

| Event | Logged Data | Log Level |
|-------|-------------|-----------|
| Login success | userId, username, IP address, traceId | INFO |
| Login failure | username (no password), traceId | WARN |
| Token refresh | userId, traceId | INFO |
| Token theft detected | userId, all tokens revoked | ERROR + ALERT |
| Password reset requested | email (generic, even if not found), traceId | INFO |
| Password reset success | userId, traceId | INFO |

### 3.11 Security Requirements

- RSA key pair is generated in memory at startup. Key rotation requires service restart (Phase 2: persistent key store).
- All authentication endpoints enforce HTTPS (TLS 1.2+) in production.
- Brute force protection: consider implementing account lockout after N failed attempts in Phase 2.
- Sensitive headers (Authorization) must never appear in application logs.

---

## FSD-004 Account Management Module

### 4.1 Functional Description

The Account Management module handles the lifecycle of customer bank accounts including automatic provisioning (triggered by Kafka events), balance enquiry, and debit/credit operations. Debit and credit operations use pessimistic write locks to ensure concurrent transfer safety.

### 4.2 Service Owner
`account-service` (Port 8083)

### 4.3 Inputs

| Operation | Input | Source |
|-----------|-------|--------|
| Create Account (auto) | UserCreatedEvent: { id, email, username } | Kafka consumer |
| Create Account (manual) | JWT sub claim (userId) | POST /accounts |
| Get Own Account | JWT sub claim (userId) | GET /accounts/me |
| Get by Account Number | accountNumber (path param) | GET /internal/accounts/{acctNum} |
| Get by UserId | userId (path param) | GET /internal/accounts/user/{userId} |
| Debit | accountNumber, amount (BigDecimal) | POST /internal/accounts/{acct}/debit |
| Credit | accountNumber, amount (BigDecimal) | POST /internal/accounts/{acct}/credit |

### 4.4 Outputs

**AccountResponse:**
```json
{
  "id": "uuid",
  "userId": "auth-uuid",
  "accountNumber": "1234567890",
  "balance": 12500000.00,
  "currency": "VND",
  "status": "ACTIVE",
  "createdAt": "2026-01-01T08:00:00Z",
  "updatedAt": "2026-06-04T10:30:00Z",
  "version": 5
}
```

### 4.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-ACCT-001 | Each customer holds exactly one account in Phase 1. Account creation is idempotent — duplicate creation returns the existing account. |
| BR-ACCT-002 | Account numbers are 10-digit numeric strings generated by a collision-resistant random algorithm, verified unique before assignment. |
| BR-ACCT-003 | Opening balance is VND 0.00. Currency default is VND (ISO 4217). |
| BR-ACCT-004 | Account status lifecycle: ACTIVE → SUSPENDED → ACTIVE (reinstatement) or ACTIVE → CLOSED (permanent). CLOSED accounts cannot be re-activated. |
| BR-ACCT-005 | Debit and credit operations use pessimistic write locks (SELECT FOR UPDATE) to prevent concurrent balance corruption. |
| BR-ACCT-006 | Optimistic locking (`@Version`) provides a secondary defence against lost updates at the JPA layer. |
| BR-ACCT-007 | Debit must fail if the account balance would go negative. The check occurs inside the locked transaction. |
| BR-ACCT-008 | Debit and credit operations require account status = ACTIVE. |
| BR-ACCT-009 | Balance is stored as NUMERIC(19,2). All arithmetic uses BigDecimal with HALF_UP rounding at scale 2. |
| BR-ACCT-010 | Internal endpoints (/internal/**) are not routed through the API Gateway and do not require JWT. They are protected by network-level access control only. |

### 4.6 Validation Rules

| Rule ID | Field | Rule |
|---------|-------|------|
| VAL-ACCT-001 | amount (debit) | Must be > 0 |
| VAL-ACCT-002 | amount (debit) | Must not exceed current balance |
| VAL-ACCT-003 | amount (credit) | Must be > 0 |
| VAL-ACCT-004 | accountNumber | Must be 10 digits |
| VAL-ACCT-005 | account status | Must be ACTIVE for debit/credit operations |

### 4.7 Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| ACCOUNT_NOT_FOUND | 404 | No account found for given userId or accountNumber |
| INSUFFICIENT_BALANCE | 400 | Balance < requested debit amount |
| ACCOUNT_NOT_ACTIVE | 400 | Account status is SUSPENDED or CLOSED |
| INVALID_AMOUNT | 400 | Amount is null, zero, or negative |
| DUPLICATE_ACCOUNT | 200 | Account already exists — returns existing (idempotent) |

### 4.8 Database Locking Strategy

```
Pessimistic Lock (SELECT FOR UPDATE):
  Repository: findByAccountNumberWithLock()
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Applied during: debit() and credit() operations
  Effect: Exclusive row lock held for duration of transaction
  Timeout: 30 seconds (configurable); throws LockTimeoutException on timeout

Optimistic Lock (@Version):
  Entity field: private Long version
  Applied to: all Account updates
  Effect: If two transactions read the same version and one commits first,
          the second receives OptimisticLockException
  Retry: Not automatic; calling service must handle exception and retry
```

### 4.9 API Specification

**POST /accounts** (JWT Required)
```
Response (200):
  AccountResponse (existing or new)
```

**GET /accounts/me** (JWT Required)
```
Response (200): AccountResponse
Response (404): { "error": "Account not found for userId: {id}" }
```

**POST /internal/accounts/{accountNumber}/debit** (No JWT — Internal Only)
```
Request Body: { "amount": 500000.00 }
Response (200): {} (empty body)
Response (400): { "error": "Insufficient balance for account: {accountNumber}" }
```

### 4.10 Audit Requirements

| Event | Logged Data | Log Level |
|-------|-------------|-----------|
| Account created | userId, accountNumber, currency, traceId | INFO |
| Account already exists (idempotent) | userId, existingAccountNumber | WARN |
| Debit initiated | accountNumber, amount, traceId | INFO |
| Debit success | accountNumber, amount, newBalance | INFO |
| Debit failed (insufficient) | accountNumber, balance, requestedAmount | WARN |
| Credit success | accountNumber, amount, newBalance | INFO |
| Account status changed | accountNumber, oldStatus, newStatus, actorId | INFO |

---

## FSD-005 Fund Transfer Module

### 5.1 Functional Description

The Fund Transfer module orchestrates real-time domestic fund transfers between accounts on the platform. It maintains a transaction state machine (PENDING → SUCCESS/FAILED), enforces idempotency via a client-supplied Idempotency-Key header, and coordinates debit/credit operations through the account-service internal API.

### 5.2 Service Owner
`transaction-service` (Port 8084)

### 5.3 Inputs

| Field | Type | Mandatory | Validation |
|-------|------|-----------|------------|
| toAccountNumber | String | Yes | 10 digits; must exist on platform; must ≠ source account |
| amount | BigDecimal | Yes | > 0; ≤ account balance; rounded to 2dp HALF_UP |
| Idempotency-Key (header) | String | No | Max 64 chars; UUID format recommended |
| JWT Authorization (header) | String | Yes | Valid RS256 JWT; sub claim = userId |

### 5.4 Outputs

**TransactionResponse:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fromAccount": "1234567890",
  "toAccount": "0987654321",
  "amount": 500000.00,
  "currency": "VND",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "2026-06-04T10:32:15Z",
  "updatedAt": "2026-06-04T10:32:16Z"
}
```

### 5.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-TRF-001 | The source account is determined from the JWT sub claim (userId). The client cannot specify the source account. |
| BR-TRF-002 | Self-transfers (fromAccount == toAccount) are rejected. |
| BR-TRF-003 | Both source and destination accounts must have status = ACTIVE. |
| BR-TRF-004 | The source account must have balance ≥ transfer amount. Balance check performed at application layer; enforced again at DB layer during debit with lock. |
| BR-TRF-005 | Idempotency: if Idempotency-Key matches an existing transaction, the original response is returned without re-execution. |
| BR-TRF-006 | Transfer state machine: PENDING (on create) → SUCCESS (on debit+credit completion) or FAILED (on any step failure). |
| BR-TRF-007 | FAILED transactions must capture failureReason in the transaction record. |
| BR-TRF-008 | The transfer correlationId in logs is set to the idempotencyKey (if provided) or a new UUID. Consistent across all log entries for the transfer. |
| BR-TRF-009 | Minimum transfer amount: VND 1,000. |

### 5.6 Transfer Execution Flow

```
1. IDEMPOTENCY CHECK:
   IF idempotency_key EXISTS in transactions table:
     RETURN existing transaction (no re-execution)
   END IF

2. RESOLVE SOURCE ACCOUNT:
   GET /internal/accounts/user/{userId}
   Extract fromAccountNumber

3. VALIDATE:
   fromAccount != toAccount              → REJECT: "Cannot transfer to same account"
   fromAccount.status == ACTIVE          → else REJECT: "Source account not ACTIVE"
   toAccount.status == ACTIVE            → else REJECT: "Destination account not ACTIVE"
   fromAccount.balance >= amount         → else REJECT: "Insufficient balance"

4. CREATE PENDING TRANSACTION:
   INSERT transaction { status: PENDING, ... }

5. EXECUTE:
   POST /internal/accounts/{fromAccount}/debit { amount }
   POST /internal/accounts/{toAccount}/credit { amount }

6. UPDATE SUCCESS:
   UPDATE transaction SET status = SUCCESS

ON EXCEPTION IN STEP 5:
   UPDATE transaction SET status = FAILED, failure_reason = ex.getMessage()
   THROW → HTTP 500 returned to client
```

### 5.7 Validation Rules

| Rule ID | Field | Rule | Error |
|---------|-------|------|-------|
| VAL-TRF-001 | toAccountNumber | Required | "Destination account number is required" |
| VAL-TRF-002 | toAccountNumber | 10-digit numeric | "Invalid account number format" |
| VAL-TRF-003 | toAccountNumber | Must exist | "Account not found: {accountNumber}" |
| VAL-TRF-004 | toAccountNumber | Must ≠ fromAccount | "Cannot transfer to the same account" |
| VAL-TRF-005 | amount | Required | "Amount is required" |
| VAL-TRF-006 | amount | > 0 | "Amount must be greater than zero" |
| VAL-TRF-007 | amount | ≥ VND 1,000 | "Minimum transfer amount is VND 1,000" |
| VAL-TRF-008 | amount | ≤ balance | "Insufficient balance. Available: {balance}" |

### 5.8 Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| SELF_TRANSFER | 400 | Source and destination account are identical |
| SOURCE_NOT_ACTIVE | 400 | Source account is SUSPENDED or CLOSED |
| DESTINATION_NOT_ACTIVE | 400 | Destination account is SUSPENDED or CLOSED |
| ACCOUNT_NOT_FOUND | 400 | Destination account number does not exist |
| INSUFFICIENT_BALANCE | 400 | Source balance < transfer amount |
| TRANSFER_FAILED | 500 | Debit/credit operation failed (service error) |
| ACCOUNT_SERVICE_UNAVAILABLE | 503 | account-service is unreachable |
| IDEMPOTENT_RETURN | 200 | Duplicate idempotency key; original result returned |

### 5.9 Idempotency Specification

```
Header: Idempotency-Key: {uuid}

Behaviour:
  1st call: Creates new transaction; stores idempotency_key with transaction.
  2nd call (same key): Finds existing transaction; returns same response.
  Different key: Treated as new transaction.

Edge cases:
  - Key submitted while 1st call still PENDING: Returns PENDING transaction.
  - Key from a FAILED transaction: Returns FAILED transaction (not retried).
  - If client wants retry after FAILED: Must use a different idempotency key.

DB enforcement: UNIQUE constraint on transactions.idempotency_key.
```

### 5.10 API Specification

**POST /transactions/transfer** (JWT Required)

```
Request:
  Authorization: Bearer {accessToken}
  Idempotency-Key: {uuid}           (recommended)
  Content-Type: application/json
  Body: {
    "toAccountNumber": "0987654321",
    "amount": 500000.00
  }

Response (200 — Success):
  {
    "success": true,
    "data": {
      "id": "...",
      "fromAccount": "1234567890",
      "toAccount": "0987654321",
      "amount": 500000.00,
      "currency": "VND",
      "status": "SUCCESS",
      "failureReason": null,
      "createdAt": "...",
      "updatedAt": "..."
    }
  }
```

### 5.11 Audit Requirements

| Event | Logged Data | Log Level |
|-------|-------------|-----------|
| Transfer start | userId, toAccount, amount, correlationId | INFO |
| Idempotent return | idempotencyKey, existingTxId | INFO |
| Transfer PENDING created | txId, fromAccount, toAccount, amount | INFO |
| Transfer SUCCESS | txId, fromAccount, toAccount, amount | INFO |
| Transfer FAILED | txId, reason, correlationId | ERROR |
| Insufficient balance | fromAccount, balance, requestedAmount | WARN |
| Account service unavailable | failed operation, correlationId | ERROR |

---

## FSD-006 Beneficiary Management Module

### 6.1 Functional Description

> Phase 2 capability. The following specification documents the planned design for Phase 2 delivery.

The Beneficiary Management module allows customers to save frequently used transfer recipients for quick access. Beneficiary records are linked to the customer's profile and validated against the interbank account directory before saving.

### 6.2 Service Owner
`beneficiary-service` (Planned — Phase 2)

### 6.3 Inputs

| Field | Type | Mandatory | Validation |
|-------|------|-----------|------------|
| accountNumber | String | Yes | 10 digits; validated via account lookup |
| beneficiaryName | String | Yes (auto-populated) | Retrieved from account lookup; not customer-input |
| bankCode | String | Yes | Must be in approved bank list |
| alias | String | No | Max 50 chars; customer-defined nickname |

### 6.4 Business Rules

| Ref | Rule |
|-----|------|
| BR-BEN-001 | Account number must be verified via account lookup before saving. |
| BR-BEN-002 | Beneficiary name is auto-populated from the account lookup result; customers cannot manually enter a beneficiary name. |
| BR-BEN-003 | Maximum 20 beneficiaries per customer (Phase 2 limit). |
| BR-BEN-004 | Duplicate account number in a customer's beneficiary list is rejected. |
| BR-BEN-005 | Beneficiary deletion is soft-delete (status = INACTIVE); records retained for audit. |

---

## FSD-007 Transaction History Module

### 7.1 Functional Description

The Transaction History module provides paginated retrieval of all transactions associated with a customer's account. It supports both customer self-service history viewing and compliance officer account investigation. Results are always sorted by createdAt descending (newest first).

### 7.2 Service Owner
`transaction-service` (Port 8084)

### 7.3 Inputs

| Parameter | Type | Mandatory | Default | Validation |
|-----------|------|-----------|---------|------------|
| accountNumber | String (query) | Yes | — | 10-digit numeric |
| page | Integer (query) | No | 0 | ≥ 0 |
| size | Integer (query) | No | 10 | 1–100 |

### 7.4 Outputs

**PagedResponse:**
```json
{
  "content": [
    {
      "id": "...",
      "fromAccount": "1234567890",
      "toAccount": "0987654321",
      "amount": 500000.00,
      "currency": "VND",
      "status": "SUCCESS",
      "failureReason": null,
      "createdAt": "2026-06-04T10:32:15Z",
      "updatedAt": "2026-06-04T10:32:16Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 45,
  "totalPages": 5
}
```

### 7.5 Business Rules

| Ref | Rule |
|-----|------|
| BR-HIST-001 | History returns all transactions where the account is either fromAccount (debit) or toAccount (credit). |
| BR-HIST-002 | Results are always sorted by createdAt DESC (newest first). |
| BR-HIST-003 | Default page size is 10; maximum is 100. Requests for size > 100 are capped. |
| BR-HIST-004 | History is read-only — no modification is permitted through history endpoints. |
| BR-HIST-005 | All transaction statuses are included: PENDING, SUCCESS, FAILED. |

### 7.6 Database Query

```sql
SELECT * FROM transactions
WHERE from_account = :accountNumber OR to_account = :accountNumber
ORDER BY created_at DESC
LIMIT :size OFFSET (:page * :size);

-- Count query for pagination:
SELECT COUNT(*) FROM transactions
WHERE from_account = :accountNumber OR to_account = :accountNumber;
```

Indexes: `idx_tx_from_account`, `idx_tx_to_account` ensure this query is efficient even with millions of transaction records.

### 7.7 API Specification

**GET /transactions/history** (JWT Required)

```
Request:
  Authorization: Bearer {accessToken}
  Query params: accountNumber=1234567890&page=0&size=10

Response (200):
  PagedResponse<TransactionResponse>
```

### 7.8 Audit Requirements

| Event | Logged Data | Log Level |
|-------|-------------|-----------|
| History requested | accountNumber, page, size, userId, traceId | INFO |
| History returned | accountNumber, totalElements, page | INFO |

---

*Document End — BCP-FSD-001 v1.0*
