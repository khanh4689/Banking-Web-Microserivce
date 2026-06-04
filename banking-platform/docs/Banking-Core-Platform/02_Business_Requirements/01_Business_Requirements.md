# Banking Core Platform
## Document 02 — Business Requirements

**Document ID:** BCP-BR-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [Business Requirements](#1-business-requirements)
2. [Non-Functional Requirements](#2-non-functional-requirements)
3. [Regulatory Requirements](#3-regulatory-requirements)

---

## 1. Business Requirements

### Notation

| Field | Description |
|-------|-------------|
| **Ref** | Unique identifier for the requirement |
| **Category** | Functional area |
| **Priority** | MoSCoW: Must Have / Should Have / Could Have / Won't Have |
| **Source** | Stakeholder who raised the requirement |
| **Acceptance Criteria** | Measurable condition to confirm satisfaction |

---

### BR-001 — Customer Self-Service Digital Registration

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-001 |
| **Category** | Customer Registration |
| **Priority** | Must Have |
| **Source** | Retail Banking Head, Product Owner |
| **Description** | The system must provide a fully digital, self-service customer registration capability accessible via web browser and mobile application without requiring a branch visit. A prospective customer must be able to submit their credentials (username, email address, password) and receive an email verification message to complete the onboarding process. |
| **Business Rule** | Username must be unique across the platform. Email address must be unique. Password must meet minimum complexity requirements. Registration must not activate the account until email verification is completed. |
| **Acceptance Criteria** | A new user can register in under 3 minutes. Email verification link is delivered within 2 minutes. Account activation occurs automatically upon email verification. |

---

### BR-002 — Email-Based Identity Verification

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-002 |
| **Category** | Customer Registration / Security |
| **Priority** | Must Have |
| **Source** | Compliance Officer, IT Security |
| **Description** | The system must verify each customer's email address before granting access to banking services. A time-limited verification token must be sent to the customer's registered email. The customer must click the verification link to activate their account. Tokens must expire after a configurable period to prevent reuse of stale links. |
| **Business Rule** | Verification tokens must be single-use and expire after 24 hours. The system must support re-sending the verification email upon customer request. A customer whose email has not been verified must not be able to log in. |
| **Acceptance Criteria** | Verified customer can log in. Unverified customer receives HTTP 401. Expired/reused tokens return a meaningful error. Resend functionality delivers a new token. |

---

### BR-003 — Secure Customer Authentication

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-003 |
| **Category** | Authentication |
| **Priority** | Must Have |
| **Source** | IT Security, CRO |
| **Description** | The system must authenticate customers using a username and password combination. Authentication must produce a short-lived access token (JWT) and a long-lived refresh token. The access token must be used to authorise all subsequent API requests. Credentials must never be stored in plaintext; password hashing must use BCrypt. |
| **Business Rule** | Access tokens expire after 10 minutes. Refresh tokens expire after 7 days. Failed login attempts must be logged. The JWT must contain the user's ID, username, and roles. |
| **Acceptance Criteria** | Successful login returns a JWT access token and refresh token. Invalid credentials return HTTP 401. JWT validates against the JWKS public key endpoint. Token payload contains correct claims. |

---

### BR-004 — JWT Token Refresh with Rotation

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-004 |
| **Category** | Authentication / Security |
| **Priority** | Must Have |
| **Source** | IT Security |
| **Description** | The system must support refresh token rotation to maintain user sessions beyond the access token lifetime without requiring re-authentication. Each refresh operation must issue a new access token and a new refresh token, and immediately revoke the previous refresh token. |
| **Business Rule** | Refresh tokens are stored as SHA-256 hashes; the raw token is never persisted. Token rotation must occur atomically. If a revoked refresh token is submitted (potential theft), the system must revoke all tokens for the affected user and require re-authentication. |
| **Acceptance Criteria** | Submitting a valid refresh token returns a new token pair. The previous refresh token is invalidated. Submitting an already-used refresh token triggers full session revocation. |

---

### BR-005 — Social Login via Google OAuth2

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-005 |
| **Category** | Authentication |
| **Priority** | Should Have |
| **Source** | Product Owner, Retail Banking Head |
| **Description** | The system must support Google OAuth2 as an alternative authentication mechanism, enabling customers to log in using their existing Google accounts without creating a separate password. New users authenticating via Google for the first time must have a user profile and account automatically provisioned. |
| **Business Rule** | Google-authenticated users do not require email verification (Google guarantees email ownership). A UserCreatedEvent must be published on first Google login. Google client ID and client secret are environment-injected secrets. |
| **Acceptance Criteria** | Customer can initiate Google OAuth2 flow and receive a JWT. New Google user has profile and account created automatically. Existing Google user logs in without duplicate profile creation. |

---

### BR-006 — Password Reset via Email

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-006 |
| **Category** | Authentication / Customer Self-Service |
| **Priority** | Must Have |
| **Source** | Product Owner, Customer Service Team |
| **Description** | The system must provide a secure self-service password reset mechanism. A customer who forgets their password must be able to request a reset link via their registered email address. The reset link must contain a time-limited, single-use token. Upon consuming the token, the customer sets a new password. |
| **Business Rule** | Password reset tokens must expire after 1 hour. Tokens must be single-use; once consumed, they cannot be reused. The system must respond with a generic success message regardless of whether the email exists, to prevent user enumeration attacks. The new password must meet minimum complexity rules. |
| **Acceptance Criteria** | A reset email is delivered within 2 minutes. Expired token returns an error. Valid token allows password change. Old credentials no longer work post-reset. |

---

### BR-007 — Automatic User Profile Provisioning

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-007 |
| **Category** | Customer Profile |
| **Priority** | Must Have |
| **Source** | Product Owner |
| **Description** | Upon successful customer registration (email verification confirmed), the system must automatically create a Customer Information File (CIF / User Profile) for the new customer without manual intervention. The profile must capture the customer's email and username from the registration event. |
| **Business Rule** | Profile creation is triggered by a UserCreatedEvent published to the Kafka `user-events` topic. Profile creation is idempotent; duplicate events must not create duplicate profiles. Initial KYC status is set to PENDING. Profile creation must not block or fail the registration flow. |
| **Acceptance Criteria** | Profile exists within 5 seconds of email verification. Duplicate Kafka events do not create duplicate profiles. KYC status is PENDING at profile creation. |

---

### BR-008 — Customer Profile Self-Management

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-008 |
| **Category** | Customer Profile |
| **Priority** | Must Have |
| **Source** | Retail Banking Head, Product Owner |
| **Description** | Authenticated customers must be able to view and update their own profile information, including full name, phone number, and avatar image. Customers must not be able to modify their email address or KYC status through self-service. |
| **Business Rule** | Only the authenticated user can modify their own profile. Email address is read-only after registration. KYC status changes are performed by authorised bank staff only. Profile updates must be timestamped. |
| **Acceptance Criteria** | Customer can GET /users/me and receive their profile. Customer can PUT /users/me to update name, phone, avatar. Attempts to update email or KYC status are rejected. |

---

### BR-009 — Automatic Bank Account Provisioning

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-009 |
| **Category** | Account Management |
| **Priority** | Must Have |
| **Source** | Operations Officer, Product Owner |
| **Description** | Upon receipt of a UserCreatedEvent, the system must automatically provision a bank account (CASA — Current Account) for the new customer. The account must be created in ACTIVE status with a zero opening balance, assigned a unique 10-digit account number, and denominated in Vietnamese Dong (VND). |
| **Business Rule** | One customer may hold exactly one account in Phase 1. Account numbers must be unique across the platform and generated using a collision-resistant algorithm. Account creation is idempotent; if an account already exists for the user, the existing account is returned. Opening balance must be VND 0.00. Default currency is VND (ISO 4217). |
| **Acceptance Criteria** | Account exists within 5 seconds of user registration event. Account number is 10 digits, unique. Account status is ACTIVE. Balance is 0.00 VND. Duplicate event does not create a second account. |

---

### BR-010 — Real-Time Balance Enquiry

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-010 |
| **Category** | Account Management |
| **Priority** | Must Have |
| **Source** | Retail Banking Head |
| **Description** | Authenticated customers must be able to retrieve their current account balance and account details in real time via the mobile or web interface. The balance displayed must reflect the current ledger balance after all posted transactions. |
| **Business Rule** | Balance must be retrieved from the live account record (no caching of balance data). Balance must be presented with 2 decimal places precision. Currency code (VND) must be displayed alongside the balance. Account status must be displayed. |
| **Acceptance Criteria** | GET /accounts/me returns current balance in under 500ms. Balance reflects all completed debits and credits. Response includes account number, status, balance, and currency. |

---

### BR-011 — Domestic Fund Transfer

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-011 |
| **Category** | Fund Transfer |
| **Priority** | Must Have |
| **Source** | Retail Banking Head, Operations Officer |
| **Description** | Authenticated customers must be able to initiate a real-time fund transfer from their account to another account on the platform by specifying the destination account number and transfer amount. The transfer must complete within 5 seconds under normal operating conditions. |
| **Business Rule** | The source account is determined from the authenticated user's JWT (userId) — it cannot be specified by the client. Self-transfers (same account number as source and destination) must be rejected. Both source and destination accounts must be in ACTIVE status. Source account must have sufficient available balance. Minimum transfer amount: VND 1,000. Transfer amount must be rounded to 2 decimal places. |
| **Acceptance Criteria** | POST /transactions/transfer with valid payload returns SUCCESS status and transaction ID. Insufficient balance returns HTTP 400. Inactive account returns HTTP 400. Self-transfer returns HTTP 400. Transaction record is persisted with PENDING → SUCCESS status progression. |

---

### BR-012 — Idempotent Transfer Processing

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-012 |
| **Category** | Fund Transfer / Reliability |
| **Priority** | Must Have |
| **Source** | IT Development Lead, Operations Officer |
| **Description** | The fund transfer API must support idempotency via a client-supplied Idempotency-Key header. If a transfer request is re-submitted with the same idempotency key (e.g., due to a network timeout causing a client retry), the system must return the original transaction result without executing a duplicate transfer. |
| **Business Rule** | Idempotency key must be unique per transfer operation. The key is stored with the transaction record in the database. Duplicate key detection must use a unique database constraint, not an application-level cache. If the key already exists, the existing transaction response is returned with HTTP 200 without modifying any account balances. |
| **Acceptance Criteria** | First request with key X creates transaction and updates balances. Second request with key X returns existing transaction without modifying balances. No duplicate charge to the source account. |

---

### BR-013 — Transaction Audit Trail

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-013 |
| **Category** | Fund Transfer / Audit |
| **Priority** | Must Have |
| **Source** | Compliance Officer, CRO |
| **Description** | Every fund transfer must create an immutable transaction record capturing the source account, destination account, amount, currency, status, timestamp, and failure reason (if applicable). Transaction records must support status transitions from PENDING to SUCCESS or FAILED. |
| **Business Rule** | Transaction records must never be deleted. FAILED transactions must include a human-readable failure reason. All monetary amounts must be stored as NUMERIC(19,2). Timestamps must be in UTC. The Trace ID must be associated with each transaction for cross-service correlation. |
| **Acceptance Criteria** | Transaction record exists for every transfer attempt. Status field reflects final outcome. FAILED records contain failureReason. Records are retrievable via transaction history API. |

---

### BR-014 — Paginated Transaction History

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-014 |
| **Category** | Transaction History |
| **Priority** | Must Have |
| **Source** | Retail Banking Head, Compliance Officer |
| **Description** | Customers must be able to view a paginated history of all transactions associated with their account, including both sent and received transfers. Results must be sorted from most recent to oldest by default. |
| **Business Rule** | History must include transactions where the account is either the source (debit) or destination (credit). Default page size is 10 records. Maximum page size is 100 records. Results must include total count, total pages, current page, and page size metadata. History must display transaction direction (debit/credit). |
| **Acceptance Criteria** | GET /transactions/history returns paginated list. Response includes pagination metadata. Transactions sorted newest-first. Both sent and received transactions are included. Large histories are paginated correctly. |

---

### BR-015 — Account Status Management

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-015 |
| **Category** | Account Management / Operations |
| **Priority** | Must Have |
| **Source** | Operations Officer, Compliance Officer |
| **Description** | Authorised bank staff (Operations Officers, Compliance Officers) must be able to change the status of a customer account. Valid status transitions include: ACTIVE → SUSPENDED (temporary restriction), ACTIVE → CLOSED (permanent closure), SUSPENDED → ACTIVE (reinstatement). A SUSPENDED or CLOSED account must not permit debit or credit operations. |
| **Business Rule** | CLOSED accounts cannot be re-activated. Status changes must be logged in the audit trail with actor ID, timestamp, and reason code. SUSPENDED accounts cannot initiate transfers but can receive credits (bank's discretion based on regulatory requirement). |
| **Acceptance Criteria** | Operations Officer can update account status via admin API. Transfer from SUSPENDED account returns HTTP 400. Status change is recorded in audit log. |

---

### BR-016 — Role-Based Access Control (RBAC)

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-016 |
| **Category** | Security / Authorisation |
| **Priority** | Must Have |
| **Source** | IT Security, CRO |
| **Description** | The system must enforce role-based access control across all API endpoints. Roles are embedded in the JWT and enforced at each service. Customers (ROLE_USER) must only access their own data. Administrative functions must be restricted to users with ROLE_ADMIN. |
| **Business Rule** | Roles: ROLE_USER (default), ROLE_ADMIN (privileged operations). A customer cannot access another customer's profile, account, or transaction history. Admin endpoints are accessible only to users with ROLE_ADMIN. Service-to-service internal endpoints are accessible without JWT (internal network only). |
| **Acceptance Criteria** | ROLE_USER attempting to access admin endpoint receives HTTP 403. JWT without correct role cannot access protected resources. Internal endpoints are not accessible through the API gateway. |

---

### BR-017 — Distributed Request Tracing

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-017 |
| **Category** | Observability / Audit |
| **Priority** | Must Have |
| **Source** | IT Development Lead, Operations Officer |
| **Description** | Every request entering the platform must be assigned a unique Trace ID. This ID must be propagated across all service boundaries and included in every log entry associated with that request, enabling end-to-end correlation of a customer interaction across microservices. |
| **Business Rule** | Trace ID is generated by the receiving service if not present in the incoming `X-Trace-Id` header. MDC must be populated at the start of each request and cleared at completion. Log format must include: timestamp, log level, service name, trace ID, thread, class, and message. |
| **Acceptance Criteria** | Every log entry contains a traceId. A single customer transaction can be traced across auth-service, account-service, and transaction-service logs using the traceId. |

---

### BR-018 — Structured Audit Logging

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-018 |
| **Category** | Audit / Compliance |
| **Priority** | Must Have |
| **Source** | Compliance Officer, CRO |
| **Description** | The API Gateway must log all inbound requests including HTTP method, URI path, source IP address, response status code, response time, and the requesting user's identifier (from JWT). These logs must be persisted and available for compliance review and security investigation. |
| **Business Rule** | Audit logs must be immutable once written. Log retention must be minimum 7 years per SBV data retention requirements. Sensitive data (passwords, full PAN, CVV) must never appear in logs. Log files must be rotatable without service interruption. |
| **Acceptance Criteria** | Audit log contains an entry for every API call. Log entry includes timestamp, method, path, status code, duration, and user identity. Log files are written to mounted volumes per service. |

---

### BR-019 — Transactional Data Consistency

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-019 |
| **Category** | Data Integrity |
| **Priority** | Must Have |
| **Source** | IT Development Lead, CRO |
| **Description** | The system must guarantee that fund transfer operations maintain strict data consistency. A debit on the source account and a credit on the destination account must either both succeed or both fail — no partial state must be persisted. Concurrent modification of account balances must be prevented. |
| **Business Rule** | Transfer operations must use database-level pessimistic write locks on account records during debit and credit operations. The transaction record must first be persisted as PENDING before any balance modification. On failure, the transaction record must be updated to FAILED with a failure reason. Optimistic locking at the JPA layer provides a secondary defence against lost updates. |
| **Acceptance Criteria** | A failed credit after a successful debit does not result in money loss (system rolls back or marks FAILED with ops alert). Concurrent transfers to/from the same account complete sequentially without balance errors. |

---

### BR-020 — Service Health Monitoring

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-020 |
| **Category** | Operations / Resilience |
| **Priority** | Must Have |
| **Source** | IT Infrastructure Team, Operations Officer |
| **Description** | Every microservice must expose a health check endpoint that can be polled by the container orchestration layer and monitoring systems to determine service readiness. The health endpoint must reflect the service's ability to serve traffic (database connectivity, dependency availability). |
| **Business Rule** | Health endpoint: GET /actuator/health. Response must include HTTP 200 with `{ "status": "UP" }` when healthy. Must be accessible without authentication. Must be used in Docker Compose health checks and Kubernetes readiness probes. |
| **Acceptance Criteria** | All services return HTTP 200 from /actuator/health when operational. Docker Compose health checks use this endpoint to gate dependent service startup. |

---

### BR-021 — Kafka Event-Driven Account and Profile Provisioning

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-021 |
| **Category** | Integration / Events |
| **Priority** | Must Have |
| **Source** | IT Development Lead |
| **Description** | Registration completion in auth-service must trigger automated provisioning of both a user profile (user-service) and a bank account (account-service) through a Kafka event. The event must only be published after the registration database transaction commits, ensuring no downstream consumers receive events for uncommitted data. |
| **Business Rule** | Topic: `user-events`. Event: `UserCreatedEvent` (payload: id, email, username). The Kafka producer uses `@TransactionalEventListener(AFTER_COMMIT)` to guarantee publication ordering. Each consumer group processes the event independently. Consumers must be idempotent to handle Kafka at-least-once delivery. |
| **Acceptance Criteria** | UserCreatedEvent is on the topic within 1 second of registration commit. user-service creates profile. account-service creates account. Neither creates duplicates on re-delivery. |

---

### BR-022 — Multi-Database Isolation per Service

| Attribute | Value |
|-----------|-------|
| **Ref** | BR-022 |
| **Category** | Architecture / Data Governance |
| **Priority** | Must Have |
| **Source** | IT Development Lead, CRO |
| **Description** | Each microservice must own its own database schema with no shared tables or direct cross-database queries. Data sharing between services must occur exclusively through APIs or Kafka events. This ensures each service can be scaled, deployed, and backed up independently. |
| **Business Rule** | auth-service → auth_db. user-service → user_db. account-service → account_db. transaction-service → transaction_db. No service may hold a foreign key to another service's database. Cross-service lookups must use REST API calls. |
| **Acceptance Criteria** | All services start with separate database connection strings. No cross-database foreign keys exist. account-service stores userId as a plain VARCHAR, not as a FK to user_db. |

---

## 2. Non-Functional Requirements

### 2.1 Performance Requirements

| Ref | Requirement | Target | Measurement Method |
|-----|-------------|--------|--------------------|
| NFR-P-001 | API response time — login | p95 < 500ms | Load test |
| NFR-P-002 | API response time — balance enquiry | p95 < 300ms | Load test |
| NFR-P-003 | API response time — fund transfer | p95 < 3,000ms | Load test |
| NFR-P-004 | API response time — transaction history | p95 < 800ms | Load test |
| NFR-P-005 | Peak concurrent users | 5,000 users | Load test |
| NFR-P-006 | Peak transaction throughput | 500 TPS sustained | Load test |
| NFR-P-007 | Kafka event propagation | Profile + account created within 5 seconds of registration | End-to-end test |
| NFR-P-008 | Database query performance | No unindexed full table scan on accounts or transactions | EXPLAIN ANALYSE |

### 2.2 Availability Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-A-001 | Platform availability (annual) | 99.9% uptime = max 8.76 hours downtime/year |
| NFR-A-002 | Planned maintenance window | Maximum 4 hours/month; off-peak hours only |
| NFR-A-003 | Single service failure isolation | Failure of one service must not cascade to others |
| NFR-A-004 | API Gateway availability | 99.95% (higher than individual services) |
| NFR-A-005 | Kafka availability | 99.9%; messages retained for 7 days on topic |

### 2.3 Scalability Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-S-001 | Horizontal scaling | Each service must scale independently without impacting others |
| NFR-S-002 | Database connection pooling | Services must use connection pools to avoid DB connection exhaustion |
| NFR-S-003 | Stateless services | No session state stored in application memory; all state in DB or JWT |
| NFR-S-004 | Load balancing | API Gateway must distribute load across multiple instances of each service |
| NFR-S-005 | Auto-scaling trigger | CPU > 70% for 5 minutes triggers additional instance provisioning |

### 2.4 Security Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-SEC-001 | Password storage | BCrypt with minimum work factor 10 |
| NFR-SEC-002 | JWT algorithm | RSA RS256 with 2048-bit key |
| NFR-SEC-003 | JWT access token lifetime | 10 minutes maximum |
| NFR-SEC-004 | HTTPS enforcement | All external communications must use TLS 1.2 or higher |
| NFR-SEC-005 | Internal service communication | Internal APIs must not be routable via API Gateway |
| NFR-SEC-006 | PII in logs | No passwords, tokens, or full account numbers in log files |
| NFR-SEC-007 | Input validation | All API inputs validated; SQL injection prevention via parameterised queries |
| NFR-SEC-008 | CSRF protection | Disabled (stateless JWT architecture; no cookie-based sessions) |
| NFR-SEC-009 | Secret management | All secrets injected via environment variables; never hardcoded in source |
| NFR-SEC-010 | Refresh token storage | SHA-256 hash only; raw token never persisted |

### 2.5 Auditability Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-AU-001 | Request audit log | 100% of API requests logged at Gateway level |
| NFR-AU-002 | Distributed trace ID | Every request carries a unique traceId across all service logs |
| NFR-AU-003 | Transaction audit | Every transfer attempt logged with source, destination, amount, status |
| NFR-AU-004 | Authentication events | All login, logout, and token refresh events logged with user ID and timestamp |
| NFR-AU-005 | Log immutability | Audit logs must not be modifiable after write |
| NFR-AU-006 | Log format | Structured JSON format suitable for ingestion into SIEM/log management |

### 2.6 Reliability & Resilience Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-R-001 | Idempotency | Fund transfers must be idempotent via Idempotency-Key |
| NFR-R-002 | Kafka consumer resilience | Failed Kafka events must be retried; consumer must not silently swallow errors |
| NFR-R-003 | Account service unavailability | Transaction service must return a clear error if account-service is unreachable |
| NFR-R-004 | Database connection failure | Services must return HTTP 503 with retry guidance if DB is unavailable |
| NFR-R-005 | Concurrent balance operations | Pessimistic locking must prevent lost updates on simultaneous debits/credits |
| NFR-R-006 | Docker health checks | All services must implement health checks; dependents must wait for health |

### 2.7 Monitoring & Observability Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-M-001 | Actuator endpoints | All services expose /actuator/health |
| NFR-M-002 | Log aggregation | Logs written to mounted volumes; compatible with Elastic Stack / Loki |
| NFR-M-003 | Structured log format | Logback JSON appender; includes timestamp, level, service, traceId, message |
| NFR-M-004 | Kafka topic monitoring | Dead-letter queue strategy defined for failed consumer events |
| NFR-M-005 | Error alerting | FAILED transaction status must trigger operational alert (Phase 2 monitoring integration) |

### 2.8 Disaster Recovery Requirements

| Ref | Requirement | Target |
|-----|-------------|--------|
| NFR-DR-001 | Recovery Time Objective (RTO) | Maximum 4 hours for full platform restoration |
| NFR-DR-002 | Recovery Point Objective (RPO) | Maximum 1 hour data loss (daily backups + transaction log replication) |
| NFR-DR-003 | Database backup | Daily automated PostgreSQL pg_dump to offsite storage |
| NFR-DR-004 | Backup retention | 30 days daily backups, 12 months monthly backups |
| NFR-DR-005 | DR test | Annual DR simulation exercise; RTO and RPO targets verified |

---

## 3. Regulatory Requirements

### 3.1 KYC Requirements

| Ref | Requirement | Regulatory Source |
|-----|-------------|-------------------|
| REG-KYC-001 | All customer accounts must undergo Know Your Customer verification before being permitted to conduct transactions above the minimum threshold | SBV Circular 23/2014 |
| REG-KYC-002 | KYC verification must collect: full legal name, date of birth, national ID or passport number, address, and contact details | SBV Circular 23/2014 |
| REG-KYC-003 | KYC status must be tracked and visible to authorised bank staff at all times | SBV Circular 23/2014 |
| REG-KYC-004 | eKYC (digital verification) must be performed by an approved third-party identity verification service | SBV Circular 16/2020 |
| REG-KYC-005 | KYC records must be retained for minimum 5 years after account closure | SBV AML Decision 20/2013 |

### 3.2 AML Requirements

| Ref | Requirement | Regulatory Source |
|-----|-------------|-------------------|
| REG-AML-001 | All domestic transfers above VND 300,000,000 (approximately USD 12,000) in a single transaction must be flagged for review | SBV AML Decision 20/2013 |
| REG-AML-002 | Transactions must be screened against the bank's sanctions list and PEP (Politically Exposed Persons) database | FATF Recommendation 6 |
| REG-AML-003 | Suspicious activity must be reported to the Anti-Money Laundering Information Centre (AMLIC) within 48 hours of detection | SBV AML Decision 20/2013 |
| REG-AML-004 | Transaction monitoring rules must be reviewable and updatable by the Compliance Officer without code changes | Internal Compliance Policy |
| REG-AML-005 | AML screening results must be stored as part of the transaction audit record | SBV AML Decision 20/2013 |

### 3.3 Transaction Monitoring Requirements

| Ref | Requirement | Regulatory Source |
|-----|-------------|-------------------|
| REG-TM-001 | The system must generate a Suspicious Transaction Report (STR) for transactions meeting defined criteria | SBV AML Decision 20/2013 |
| REG-TM-002 | Transaction velocity monitoring: more than 10 transactions per day from a single account must trigger a review flag | Internal Risk Policy |
| REG-TM-003 | Round-amount monitoring: transactions of exactly VND 500,000,000 or exact multiples may indicate structuring | Internal Risk Policy |
| REG-TM-004 | Dormant account activity: any transaction on an account with no activity for 12+ months must be flagged | Internal Risk Policy |

### 3.4 Audit Trail Requirements

| Ref | Requirement | Regulatory Source |
|-----|-------------|-------------------|
| REG-AT-001 | An immutable audit log must be maintained for all customer-initiated actions, system-generated events, and administrative changes | SBV Circular 19/2016 |
| REG-AT-002 | Audit records must include: actor identity, action type, timestamp (UTC), affected entity, before/after state for data changes | SBV Circular 19/2016 |
| REG-AT-003 | Audit logs must be protected against unauthorised modification or deletion | SBV Circular 19/2016 |
| REG-AT-004 | Audit logs must be available to authorised regulatory examiners upon request within 24 hours | SBV Circular 19/2016 |

### 3.5 Data Retention Requirements

| Ref | Requirement | Regulatory Source |
|-----|-------------|-------------------|
| REG-DR-001 | Transaction records must be retained for minimum 10 years | SBV Circular 35/2015 |
| REG-DR-002 | Customer KYC documents must be retained for minimum 5 years after account closure | SBV AML Decision 20/2013 |
| REG-DR-003 | Audit logs must be retained for minimum 7 years | SBV Circular 19/2016 |
| REG-DR-004 | Authentication and access logs must be retained for minimum 3 years | Internal Security Policy |
| REG-DR-005 | All retained data must be stored within Vietnamese territorial borders | Cybersecurity Law 2018, Article 26 |

---

*Document End — BCP-BR-001 v1.0*
