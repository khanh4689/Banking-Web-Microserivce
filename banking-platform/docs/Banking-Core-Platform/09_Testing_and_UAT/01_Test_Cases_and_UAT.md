# Banking Core Platform
## Document 09 — Testing and UAT

**Document ID:** BCP-TST-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team / QA Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [Testing Strategy](#1-testing-strategy)
2. [Test Cases — Customer Registration & Authentication](#2-test-cases--customer-registration--authentication)
3. [Test Cases — User Profile & eKYC](#3-test-cases--user-profile--ekyc)
4. [Test Cases — Account Management](#4-test-cases--account-management)
5. [Test Cases — Fund Transfer](#5-test-cases--fund-transfer)
6. [Test Cases — Transaction History](#6-test-cases--transaction-history)
7. [Boundary & Negative Test Cases](#7-boundary--negative-test-cases)
8. [UAT Scenarios](#8-uat-scenarios)
9. [UAT Sign-Off Template](#9-uat-sign-off-template)

---

## 1. Testing Strategy

### 1.1 Test Types

| Test Type | Scope | Owner | Tooling |
|-----------|-------|-------|---------|
| Unit Tests | Service layer business logic | Development Team | JUnit 5, Mockito |
| Integration Tests | Service + database interactions | Development Team | Spring Boot Test, Testcontainers |
| API Tests | REST endpoint contracts | QA Team | Postman / REST Assured |
| End-to-End Tests | Full user journeys across services | QA Team | Postman + Docker Compose environment |
| Performance Tests | Throughput and latency under load | Performance Engineer | JMeter / k6 |
| Security Tests | Authentication, authorisation, input validation | Security Team | OWASP ZAP, manual penetration testing |
| UAT | Business scenario validation | Business Analysts + End Users | Manual; guided by UAT scripts |

### 1.2 Test Environment

| Environment | Description | Database | Purpose |
|-------------|-------------|----------|---------|
| DEV | Local Docker Compose | Ephemeral PostgreSQL | Developer unit + integration testing |
| SIT (System Integration Testing) | Dedicated server; all services deployed | Dedicated PostgreSQL instance | Functional and API testing |
| UAT | Isolated environment; mirrors production config | UAT PostgreSQL (data masked) | Business user acceptance testing |
| PROD | Production | Hardened PostgreSQL cluster | Live operation |

### 1.3 Test Case ID Convention

`TC-[MODULE]-[NNN]` where MODULE = REG (Registration), AUTH (Authentication), USR (User Profile), ACCT (Account), TRF (Transfer), HIST (History), BDRY (Boundary)

---

## 2. Test Cases — Customer Registration & Authentication

### TC-REG-001 — Successful Customer Registration

| Attribute | Value |
|-----------|-------|
| Test ID | TC-REG-001 |
| Module | Customer Registration |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | No existing account for username "testuser001" or email "test001@example.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/register with `{ "username": "testuser001", "email": "test001@example.com", "password": "TestPass1" }` | HTTP 200; response body: `{ "message": "User registered successfully. Please verify your email." }` |
| 2 | Verify auth_db.users record | users.username = "testuser001"; users.email = "test001@example.com"; users.enabled = false; users.email_verified = false; users.role = "ROLE_USER" |
| 3 | Verify password storage | users.password_hash starts with `$2a$` (BCrypt prefix); does NOT equal "TestPass1" |
| 4 | Verify verification email dispatched | email_verification_tokens record exists for the user; token is non-null UUID; expires_at = created_at + 24 hours; used = false |
| 5 | Verify Kafka event NOT yet published | No UserCreatedEvent on user-events topic (not published until email verified) |

**Pass Criteria:** All 5 steps produce expected results.

---

### TC-REG-002 — Duplicate Username Registration

| Attribute | Value |
|-----------|-------|
| Test ID | TC-REG-002 |
| Module | Customer Registration |
| Type | Negative |
| Priority | High |
| Pre-conditions | User "testuser001" already exists in auth_db |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/register with `{ "username": "testuser001", "email": "newuser@example.com", "password": "TestPass1" }` | HTTP 409 Conflict |
| 2 | Verify response body | Contains: `"error": "Username already taken."` or equivalent |
| 3 | Verify no new record created | auth_db.users COUNT remains unchanged |

---

### TC-REG-003 — Email Verification Success

| Attribute | Value |
|-----------|-------|
| Test ID | TC-REG-003 |
| Module | Customer Registration |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | TC-REG-001 completed; valid unexpired token in email_verification_tokens |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /auth/verify-email?token={validToken} | HTTP 200; `"Email verified successfully."` |
| 2 | Verify auth_db.users record updated | users.enabled = true; users.email_verified = true |
| 3 | Verify token marked as used | email_verification_tokens.used = true |
| 4 | Verify Kafka event published | UserCreatedEvent on user-events topic with correct id, email, username |
| 5 | Wait 5 seconds; verify user-service profile | users_profile record exists with auth_id = users.id; kyc_status = "PENDING" |
| 6 | Wait 5 seconds; verify account-service account | accounts record exists with user_id = users.id; balance = 0.00; status = "ACTIVE"; account_number is 10-digit |

**Pass Criteria:** All 6 steps produce expected results.

---

### TC-REG-004 — Email Verification with Expired Token

| Attribute | Value |
|-----------|-------|
| Test ID | TC-REG-004 |
| Module | Customer Registration |
| Type | Negative |
| Priority | High |
| Pre-conditions | A verification token exists with expires_at < NOW() |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /auth/verify-email?token={expiredToken} | HTTP 400 Bad Request |
| 2 | Verify response body | Contains "Invalid or expired verification token." |
| 3 | Verify user account still inactive | users.enabled = false; users.email_verified = false |

---

### TC-AUTH-001 — Successful Login

| Attribute | Value |
|-----------|-------|
| Test ID | TC-AUTH-001 |
| Module | Authentication |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | Customer "testuser001" is registered and email-verified |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/login with `{ "username": "testuser001", "password": "TestPass1" }` | HTTP 200 |
| 2 | Verify response body fields | accessToken: non-null String; refreshToken: non-null String; expiresIn: positive Long (600) |
| 3 | Decode JWT accessToken (base64 decode payload) | sub = users.id; username = "testuser001"; roles = ["ROLE_USER"]; exp - iat = 600 seconds |
| 4 | Verify refresh_tokens record | refresh_tokens.user_id = users.id; revoked = false; expires_at > NOW(); token_hash = SHA-256(refreshToken) |

**Pass Criteria:** All 4 steps pass.

---

### TC-AUTH-002 — Login with Invalid Password

| Attribute | Value |
|-----------|-------|
| Test ID | TC-AUTH-002 |
| Module | Authentication |
| Type | Negative |
| Priority | Critical |
| Pre-conditions | Customer "testuser001" exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/login with `{ "username": "testuser001", "password": "WrongPassword" }` | HTTP 401 |
| 2 | Verify response body | Contains "Invalid credentials" |
| 3 | Verify no token issued | No refresh_tokens record created with this attempt |
| 4 | Verify log | LOG entry `LOGIN_FAILED - username: testuser001` exists; password NOT in log |

---

### TC-AUTH-003 — Refresh Token Rotation

| Attribute | Value |
|-----------|-------|
| Test ID | TC-AUTH-003 |
| Module | Authentication |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | TC-AUTH-001 completed; have valid refreshToken |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/refresh with `{ "refreshToken": "{refreshToken from TC-AUTH-001}" }` | HTTP 200 |
| 2 | Verify response | New accessToken and new refreshToken returned; both non-null and different from original |
| 3 | Verify old refresh token revoked | refresh_tokens record for old hash: revoked = true |
| 4 | Verify new refresh token stored | New refresh_tokens record: revoked = false; expires_at 7 days from now |
| 5 | Attempt to reuse old refresh token | POST /auth/refresh with old token → HTTP 401 |

---

### TC-AUTH-004 — Refresh Token Theft Detection

| Attribute | Value |
|-----------|-------|
| Test ID | TC-AUTH-004 |
| Module | Authentication |
| Type | Negative (Security) |
| Priority | Critical |
| Pre-conditions | A refresh token has been rotated (TC-AUTH-003); old token = revoked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/refresh with the already-used (revoked) refresh token | HTTP 401 |
| 2 | Verify ALL tokens for the user are revoked | All refresh_tokens records for user: revoked = true |
| 3 | Attempt login with valid credentials | Must require fresh username/password login; existing tokens cannot be used |

---

### TC-AUTH-005 — Password Reset Flow

| Attribute | Value |
|-----------|-------|
| Test ID | TC-AUTH-005 |
| Module | Authentication |
| Type | Positive |
| Priority | High |
| Pre-conditions | Customer "testuser001" exists and is email-verified |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /auth/forgot-password with `{ "email": "test001@example.com" }` | HTTP 200; "Password reset email sent." |
| 2 | Verify password_reset_tokens record | Token exists; expires_at = NOW() + 1 hour; used = false |
| 3 | POST /auth/reset-password with `{ "token": "{resetToken}", "newPassword": "NewSecure1" }` | HTTP 200; "Password reset successfully." |
| 4 | Verify password updated | users.password_hash has changed |
| 5 | Verify token is consumed | password_reset_tokens.used = true |
| 6 | POST /auth/login with new password | HTTP 200; new tokens issued successfully |
| 7 | POST /auth/login with old password | HTTP 401; "Invalid credentials" |

---

## 3. Test Cases — User Profile & eKYC

### TC-USR-001 — Get Own Profile

| Attribute | Value |
|-----------|-------|
| Test ID | TC-USR-001 |
| Module | User Profile |
| Type | Positive |
| Priority | High |
| Pre-conditions | TC-REG-003 completed; TC-AUTH-001 completed; have valid accessToken |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /users/me with Authorization: Bearer {accessToken} | HTTP 200 |
| 2 | Verify response fields | id: UUID; authId: matches users.id; email: "test001@example.com"; kycStatus: "PENDING"; fullName: null; phone: null |

---

### TC-USR-002 — Update Profile

| Attribute | Value |
|-----------|-------|
| Test ID | TC-USR-002 |
| Module | User Profile |
| Type | Positive |
| Priority | High |
| Pre-conditions | TC-USR-001 completed |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | PUT /users/me with `{ "fullName": "Nguyen Van A", "phone": "+84912345678" }` | HTTP 200 |
| 2 | Verify response | fullName: "Nguyen Van A"; phone: "+84912345678"; updatedAt > createdAt |
| 3 | GET /users/me | Returns updated profile |

---

### TC-USR-003 — Customer Cannot Update KYC Status

| Attribute | Value |
|-----------|-------|
| Test ID | TC-USR-003 |
| Module | User Profile / Security |
| Type | Negative |
| Priority | High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | PUT /users/me with `{ "kycStatus": "VERIFIED" }` | HTTP 200 (field ignored) OR HTTP 400 |
| 2 | GET /users/me | kycStatus remains "PENDING"; not changed to "VERIFIED" |

---

## 4. Test Cases — Account Management

### TC-ACCT-001 — Account Auto-Provisioned After Registration

| Attribute | Value |
|-----------|-------|
| Test ID | TC-ACCT-001 |
| Module | Account Management |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | TC-REG-003 completed |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /accounts/me with valid accessToken | HTTP 200 |
| 2 | Verify response | accountNumber: 10-digit string; balance: 0.00; currency: "VND"; status: "ACTIVE" |
| 3 | Verify DB record | account_db.accounts: user_id = users.id; version = 0 |

---

### TC-ACCT-002 — Manual Account Creation (Idempotent)

| Attribute | Value |
|-----------|-------|
| Test ID | TC-ACCT-002 |
| Module | Account Management |
| Type | Positive (Idempotency) |
| Priority | Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /accounts (customer already has account) | HTTP 200 |
| 2 | Verify response | Same accountNumber as existing account; no new record created |
| 3 | Verify DB count | COUNT(*) FROM accounts WHERE user_id = ? returns exactly 1 |

---

### TC-ACCT-003 — Account Debit Successful

| Attribute | Value |
|-----------|-------|
| Test ID | TC-ACCT-003 |
| Module | Account Management (Internal) |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | Account "1234567890" exists with balance = 1,000,000.00 VND |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /internal/accounts/1234567890/debit with `{ "amount": 300000.00 }` | HTTP 200 |
| 2 | Verify balance updated | accounts.balance = 700000.00; accounts.version incremented by 1 |

---

### TC-ACCT-004 — Account Debit Fails — Insufficient Balance

| Attribute | Value |
|-----------|-------|
| Test ID | TC-ACCT-004 |
| Module | Account Management (Internal) |
| Type | Negative |
| Priority | Critical |
| Pre-conditions | Account exists with balance = 100,000.00 VND |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /internal/accounts/{acct}/debit with `{ "amount": 200000.00 }` | HTTP 400 |
| 2 | Verify response | Contains "Insufficient balance" |
| 3 | Verify balance unchanged | accounts.balance remains 100,000.00 |

---

## 5. Test Cases — Fund Transfer

### TC-TRF-001 — Successful Fund Transfer

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-001 |
| Module | Fund Transfer |
| Type | Positive |
| Priority | Critical |
| Pre-conditions | Customer A: balance = 2,000,000.00; Customer B: ACTIVE account "0987654321" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with `{ "toAccountNumber": "0987654321", "amount": 500000.00 }` + Idempotency-Key: "key-001" | HTTP 200 |
| 2 | Verify response | status: "SUCCESS"; fromAccount: A's number; toAccount: "0987654321"; amount: 500000.00; failureReason: null |
| 3 | Verify source balance | A's balance = 1,500,000.00 |
| 4 | Verify destination balance | B's balance increased by 500,000.00 |
| 5 | Verify transaction record | transactions: status = "SUCCESS"; idempotency_key = "key-001" |

---

### TC-TRF-002 — Idempotent Transfer (Duplicate Key)

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-002 |
| Module | Fund Transfer |
| Type | Positive (Idempotency) |
| Priority | Critical |
| Pre-conditions | TC-TRF-001 completed; Idempotency-Key "key-001" already used |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with same `{ toAccountNumber, amount }` + Idempotency-Key: "key-001" | HTTP 200 |
| 2 | Verify response | Same transaction ID as TC-TRF-001 response |
| 3 | Verify balances UNCHANGED from TC-TRF-001 | No additional debit or credit applied |
| 4 | Verify transaction count | COUNT(*) WHERE idempotency_key = "key-001" = 1 (no duplicate) |

---

### TC-TRF-003 — Transfer Fails — Insufficient Balance

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-003 |
| Module | Fund Transfer |
| Type | Negative |
| Priority | Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with amount > source balance | HTTP 400 |
| 2 | Verify response | "Insufficient balance. Available: {balance}" |
| 3 | Verify no transaction record created | No PENDING or FAILED record in transactions table |
| 4 | Verify source balance unchanged | Balance = same as before attempt |

---

### TC-TRF-004 — Transfer Fails — Self-Transfer

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-004 |
| Module | Fund Transfer |
| Type | Negative |
| Priority | High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with `{ "toAccountNumber": "{own_account_number}", "amount": 100000 }` | HTTP 400 |
| 2 | Verify response | "Cannot transfer to the same account" |

---

### TC-TRF-005 — Transfer Fails — Destination Account SUSPENDED

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-005 |
| Module | Fund Transfer |
| Type | Negative |
| Priority | High |
| Pre-conditions | Destination account "0987654321" has status = SUSPENDED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer to "0987654321" | HTTP 400 |
| 2 | Verify response | "Destination account is not ACTIVE. Current status: SUSPENDED" |

---

### TC-TRF-006 — Transfer Fails — Destination Account Not Found

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-006 |
| Module | Fund Transfer |
| Type | Negative |
| Priority | High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with `{ "toAccountNumber": "0000000000" }` | HTTP 400 |
| 2 | Verify response | "Account not found: 0000000000" |

---

### TC-TRF-007 — Transfer — FAILED State on Service Error

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-007 |
| Module | Fund Transfer |
| Type | Negative (Error Scenario) |
| Priority | Critical |
| Pre-conditions | Simulate account-service failure during credit operation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Initiate transfer; account-service fails during credit call | HTTP 500 |
| 2 | Verify transaction record | Status = FAILED; failureReason = error message |
| 3 | Verify no money lost | Source balance may have been debited; operations alert required for reconciliation |
| 4 | Check logs | TRANSFER_FAILED log entry with txId and correlationId |

---

### TC-TRF-008 — Transfer Amount Rounding

| Attribute | Value |
|-----------|-------|
| Test ID | TC-TRF-008 |
| Module | Fund Transfer |
| Type | Boundary |
| Priority | Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /transactions/transfer with `{ "amount": 100000.555 }` | HTTP 200 (if balance sufficient) |
| 2 | Verify stored amount | transactions.amount = 100000.56 (HALF_UP rounding) |
| 3 | Verify debit amount | source balance reduced by exactly 100000.56 |

---

## 6. Test Cases — Transaction History

### TC-HIST-001 — Retrieve Transaction History

| Attribute | Value |
|-----------|-------|
| Test ID | TC-HIST-001 |
| Module | Transaction History |
| Type | Positive |
| Priority | High |
| Pre-conditions | Customer has at least 5 transactions (sent and received) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /transactions/history?accountNumber={acct}&page=0&size=10 | HTTP 200 |
| 2 | Verify response structure | content: array of transactions; page: 0; size: 10; totalElements: ≥ 5; totalPages: ≥ 1 |
| 3 | Verify sort order | content[0].createdAt > content[1].createdAt (newest first) |
| 4 | Verify both directions | content includes transactions where account is fromAccount AND toAccount |
| 5 | Verify all statuses included | SUCCESS, FAILED, PENDING all appear if applicable |

---

### TC-HIST-002 — Pagination Boundary

| Attribute | Value |
|-----------|-------|
| Test ID | TC-HIST-002 |
| Module | Transaction History |
| Type | Boundary |
| Priority | Medium |
| Pre-conditions | Customer has exactly 25 transactions |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | GET /transactions/history?page=0&size=10 | totalElements: 25; totalPages: 3; content.length: 10 |
| 2 | GET /transactions/history?page=2&size=10 | content.length: 5; page: 2 |
| 3 | GET /transactions/history?page=3&size=10 | HTTP 200; content: [] (empty); page: 3 |

---

## 7. Boundary & Negative Test Cases

### TC-BDRY-001 — Minimum Transfer Amount

| Test ID | TC-BDRY-001 | Type | Boundary |
|---------|-------------|------|----------|
| Action | POST /transactions/transfer with `{ "amount": 1000 }` (exact minimum) |
| Expected | HTTP 200; transfer succeeds if balance sufficient |

### TC-BDRY-002 — Below Minimum Transfer Amount

| Test ID | TC-BDRY-002 | Type | Negative Boundary |
|---------|-------------|------|---------|
| Action | POST /transactions/transfer with `{ "amount": 999 }` |
| Expected | HTTP 400; "Minimum transfer amount is VND 1,000" |

### TC-BDRY-003 — Zero Amount Transfer

| Test ID | TC-BDRY-003 | Type | Negative |
|---------|-------------|------|---------|
| Action | POST /transactions/transfer with `{ "amount": 0 }` |
| Expected | HTTP 400; field validation error |

### TC-BDRY-004 — Negative Amount Transfer

| Test ID | TC-BDRY-004 | Type | Negative |
|---------|-------------|------|---------|
| Action | POST /transactions/transfer with `{ "amount": -500000 }` |
| Expected | HTTP 400; field validation error |

### TC-BDRY-005 — Maximum Page Size

| Test ID | TC-BDRY-005 | Type | Boundary |
|---------|-------------|------|---------|
| Action | GET /transactions/history?size=100 |
| Expected | HTTP 200; returns up to 100 records |

### TC-BDRY-006 — Page Size Exceeds Maximum

| Test ID | TC-BDRY-006 | Type | Boundary |
|---------|-------------|------|---------|
| Action | GET /transactions/history?size=101 |
| Expected | HTTP 200; size capped at 100; OR HTTP 400 (validation) |

### TC-BDRY-007 — Username Minimum Length

| Test ID | TC-BDRY-007 | Type | Boundary |
|---------|-------------|------|---------|
| Action | POST /auth/register with `{ "username": "ab" }` (2 chars, below minimum) |
| Expected | HTTP 400; "Username must be 3–50 characters" |

### TC-BDRY-008 — Username Maximum Length

| Test ID | TC-BDRY-008 | Type | Boundary |
|---------|-------------|------|---------|
| Action | POST /auth/register with username = 51-character string |
| Expected | HTTP 400; "Username must be 3–50 characters" |

### TC-BDRY-009 — Exact Maximum Transfer (Account Balance)

| Test ID | TC-BDRY-009 | Type | Boundary |
|---------|-------------|------|---------|
| Pre-condition | Account balance = 500,000.00 exactly |
| Action | POST /transactions/transfer with `{ "amount": 500000.00 }` |
| Expected | HTTP 200; transfer SUCCESS; balance becomes 0.00 |

### TC-BDRY-010 — JWT with Expired Access Token

| Test ID | TC-BDRY-010 | Type | Security Boundary |
|---------|-------------|------|---------|
| Pre-condition | Access token issued 11+ minutes ago (past 10-min expiry) |
| Action | GET /accounts/me with expired JWT |
| Expected | HTTP 401; token expired error |

### TC-BDRY-011 — Invalid Account Number Format (11 digits)

| Test ID | TC-BDRY-011 | Type | Negative Boundary |
|---------|-------------|------|---------|
| Action | POST /transactions/transfer with `{ "toAccountNumber": "12345678901" }` (11 digits) |
| Expected | HTTP 400; "Invalid account number format" |

### TC-BDRY-012 — SQL Injection Attempt in Account Number

| Test ID | TC-BDRY-012 | Type | Security |
|---------|-------------|------|---------|
| Action | POST /transactions/transfer with `{ "toAccountNumber": "'; DROP TABLE accounts;--" }` |
| Expected | HTTP 400; input rejected; no SQL executed; no database impact |

---

## 8. UAT Scenarios

### UAT-001 — End-to-End Customer Onboarding

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-001 |
| Business Process | Customer Registration, Email Verification, First Login |
| Actor | New Customer (Business User) |
| Priority | Critical |

**Scenario:**
A new customer registers on the banking platform via the web interface, verifies their email, and logs in for the first time.

**Pre-conditions:** No existing account for the test customer.

| Step | Actor | Action | Expected Business Outcome | Pass/Fail | Comments |
|------|-------|--------|--------------------------|-----------|----------|
| 1 | Customer | Navigate to /register; enter valid username, email, password | Registration form submitted successfully | | |
| 2 | Customer | Check email inbox for verification message | Email received within 2 minutes; verification link present | | |
| 3 | Customer | Click verification link in email | "Email verified successfully" message displayed | | |
| 4 | Customer | Navigate to /login; enter credentials | JWT token received; redirected to dashboard | | |
| 5 | Customer | View dashboard | Account number displayed; balance = ₫0.00; KYC status = PENDING | | |

**Pass Criteria:** Customer reaches the dashboard with an active account within 10 minutes of starting registration.

---

### UAT-002 — Fund Transfer — Happy Path

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-002 |
| Business Process | Fund Transfer |
| Actor | Customer |
| Priority | Critical |

**Pre-conditions:** Customer A has balance ≥ ₫1,000,000; Customer B has an ACTIVE account.

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer A | Navigate to Transfer screen | Transfer form displayed; source account and balance visible |  |
| 2 | Customer A | Enter Customer B's account number | Account holder name auto-populated |  |
| 3 | Customer A | Enter amount ₫500,000; click Review | Confirmation screen shows: from, to, amount, new balance |  |
| 4 | Customer A | Confirm transfer | "Transfer Successful" displayed; transaction reference shown |  |
| 5 | Customer A | View transaction history | New transaction entry shows SENT ₫500,000; status SUCCESS |  |
| 6 | Customer B | View balance | Balance increased by ₫500,000 |  |

---

### UAT-003 — Transfer Rejected — Insufficient Balance

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-003 |
| Business Process | Fund Transfer — Error Path |
| Actor | Customer |
| Priority | Critical |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Enter transfer amount exceeding current balance | "Insufficient balance" error displayed on transfer screen |  |
| 2 | Customer | Verify source account balance unchanged | Balance reflects no change in dashboard |  |
| 3 | Customer | Check transaction history | No failed transaction record for this attempt (rejected before processing) |  |

---

### UAT-004 — Password Reset Self-Service

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-004 |
| Business Process | Password Reset |
| Actor | Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Navigate to /forgot-password; enter registered email | "Password reset email sent" message |  |
| 2 | Customer | Check email; click reset link | Password reset form displayed |  |
| 3 | Customer | Enter new password; confirm | "Password reset successfully" message |  |
| 4 | Customer | Login with OLD password | Login rejected: "Invalid credentials" |  |
| 5 | Customer | Login with NEW password | Login successful; dashboard displayed |  |

---

### UAT-005 — KYC Verification by Operations Officer

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-005 |
| Business Process | eKYC Verification |
| Actor | Operations Officer, Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Submit KYC documents | kycStatus changes to "IN_REVIEW"; customer informed |  |
| 2 | Operations Officer | Log in to admin portal; find customer | Customer record shows kycStatus = IN_REVIEW; documents accessible |  |
| 3 | Operations Officer | Review documents; approve | kycStatus updated to VERIFIED; audit log entry created |  |
| 4 | Customer | Check profile | kycStatus displays "VERIFIED" |  |
| 5 | Customer | Check email | Approval notification email received |  |

---

### UAT-006 — Account Freeze by Operations Officer

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-006 |
| Business Process | Account Freeze |
| Actor | Operations Officer, Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Operations Officer | Navigate to account management; find target account | Account details displayed: status = ACTIVE |  |
| 2 | Operations Officer | Select Freeze; enter reason = FRAUD; add notes; submit | Account status = SUSPENDED; audit log created |  |
| 3 | Customer | Attempt to initiate transfer | Transfer rejected: "Account is not ACTIVE" error shown |  |
| 4 | Customer | Check email | Account restriction notification received (no AML details) |  |
| 5 | Operations Officer | Reinstate account: status → ACTIVE | Account unfrozen; audit log updated |  |
| 6 | Customer | Retry transfer | Transfer succeeds normally |  |

---

### UAT-007 — Transaction History Review by Compliance Officer

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-007 |
| Business Process | Transaction History — Compliance Review |
| Actor | Compliance Officer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Compliance Officer | Log in with ROLE_COMPLIANCE credentials | Access granted |  |
| 2 | Compliance Officer | Navigate to customer account history | Full paginated transaction history retrieved |  |
| 3 | Compliance Officer | Filter by date range (last 30 days) | Filtered results shown; pagination metadata correct |  |
| 4 | Compliance Officer | Identify a FAILED transaction | failureReason visible; source and destination accounts visible |  |
| 5 | Compliance Officer | Export history (Phase 2 feature) | CSV/PDF download initiated |  |

---

### UAT-008 — Google OAuth2 Social Login

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-008 |
| Business Process | Authentication via Google |
| Actor | New Customer |
| Priority | Medium |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Click "Sign in with Google" | Redirected to Google authorisation page |  |
| 2 | Customer | Authorise with Google account | Redirected back to platform dashboard |  |
| 3 | Customer | View dashboard | New account created; balance = ₫0.00; kycStatus = PENDING |  |
| 4 | Customer | Repeat Google login | Existing account displayed (no duplicate) |  |

---

### UAT-009 — Session Expiry and Token Refresh

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-009 |
| Business Process | Session Management |
| Actor | Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Log in; note access token expiry (10 min) | Dashboard displayed |  |
| 2 | Customer | Wait 11 minutes without activity | Access token expires |  |
| 3 | Customer | Attempt to view account | Client silently refreshes token using refresh token |  |
| 4 | Customer | View account | Balance displayed without re-login prompt |  |
| 5 | Customer | After 7 days: attempt any action | Refresh token expired; customer redirected to login screen |  |

---

### UAT-010 — Duplicate Transfer Prevention (Idempotency)

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-010 |
| Business Process | Fund Transfer — Duplicate Prevention |
| Actor | Customer, System |
| Priority | Critical |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | System | Simulate network retry: transfer request sent twice with same Idempotency-Key | Second request returns same transaction ID |  |
| 2 | Customer | Check balance | Balance debited only ONCE (not twice) |  |
| 3 | Customer | Check transaction history | Only ONE transaction record exists for the idempotency key |  |

---

### UAT-011 — Role-Based Access Enforcement

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-011 |
| Business Process | Security — RBAC |
| Actor | Customer, Bank Administrator |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer (ROLE_USER) | Attempt GET /users/admin | HTTP 403 Forbidden displayed |  |
| 2 | Customer | Attempt to access another customer's profile | HTTP 403 or 404 returned |  |
| 3 | Bank Admin (ROLE_ADMIN) | Access GET /users/admin | HTTP 200; admin data returned |  |
| 4 | External attacker | Call /internal/accounts/... via public URL | HTTP 404 (route not configured in Gateway) |  |

---

### UAT-012 — Distributed Trace Visibility

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-012 |
| Business Process | Observability — Trace ID |
| Actor | IT Operations Engineer |
| Priority | Medium |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Operations Engineer | Execute a fund transfer | Note transaction UUID in response |  |
| 2 | Operations Engineer | Search auth-service log file for traceId | TRANSFER_START, TRANSFER_PENDING, TRANSFER_SUCCESS log entries found with same traceId |  |
| 3 | Operations Engineer | Search account-service log file for same traceId | WITHDRAW_START, WITHDRAW_SUCCESS, DEPOSIT_SUCCESS entries found |  |
| 4 | Operations Engineer | Cross-reference timestamps | All log entries from the same transfer have consistent traceId |  |

---

### UAT-013 — Service Health Check

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-013 |
| Business Process | Operations — Health Monitoring |
| Actor | IT Operations Engineer |
| Priority | Medium |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Operations Engineer | GET http://localhost:8082/actuator/health | `{ "status": "UP" }` returned |  |
| 2 | Operations Engineer | GET http://localhost:8081/actuator/health | `{ "status": "UP" }` returned |  |
| 3 | Operations Engineer | GET http://localhost:8083/actuator/health | `{ "status": "UP" }` returned |  |
| 4 | Operations Engineer | GET http://localhost:8084/actuator/health | `{ "status": "UP" }` returned |  |
| 5 | Operations Engineer | Simulate DB outage; re-check health | Status reflects DOWN or degraded |  |

---

### UAT-014 — Failed Transaction Visibility to Customer

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-014 |
| Business Process | Transaction History — Error Transparency |
| Actor | Customer |
| Priority | Medium |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Attempt transfer to SUSPENDED account (which fails) | Transfer failure message displayed |  |
| 2 | Customer | Navigate to transaction history | FAILED transaction visible in history |  |
| 3 | Customer | View FAILED transaction details | failureReason displayed: "Destination account is not ACTIVE" |  |

---

### UAT-015 — KYC Rejection with Reason

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-015 |
| Business Process | eKYC Rejection |
| Actor | Operations Officer, Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Operations Officer | Review KYC; select REJECT; enter reason = "ID document expired" | kycStatus = REJECTED; reason recorded |  |
| 2 | Customer | Check email | Rejection notification with reason received |  |
| 3 | Customer | View profile | kycStatus = REJECTED displayed |  |
| 4 | Customer | Resubmit documents | kycStatus transitions back to IN_REVIEW |  |

---

### UAT-016 — Concurrent Transfer Safety

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-016 |
| Business Process | Fund Transfer — Concurrency |
| Actor | IT Operations Engineer (simulated concurrent users) |
| Priority | Critical |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Engineer | Account A has balance = ₫1,000,000 | Setup confirmed |  |
| 2 | Engineer | Simultaneously send 2 transfer requests from Account A: ₫600,000 each | One should succeed; one should fail (insufficient balance) |  |
| 3 | Engineer | Verify A's balance | Balance = ₫400,000 (only one debit applied; no negative balance) |  |
| 4 | Engineer | Check transaction records | One SUCCESS; one FAILED with "Insufficient balance" |  |

---

### UAT-017 — Resend Verification Email

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-017 |
| Business Process | Registration — Resend Email |
| Actor | Customer |
| Priority | Medium |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Register; do not click verification link | Account unverified |  |
| 2 | Customer | POST /auth/resend-verification-email with { "email": ... } | New email sent; old token invalidated |  |
| 3 | Customer | Click original (old) link | HTTP 400; "Invalid or expired token" |  |
| 4 | Customer | Click new link | Email verified; account activated |  |

---

### UAT-018 — Zero Balance Account Transfer Attempt

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-018 |
| Business Process | Fund Transfer — Edge Case |
| Actor | Customer |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Customer | Account has balance = ₫0.00 | Dashboard shows ₫0.00 |  |
| 2 | Customer | Attempt to transfer ₫1,000 (minimum) | HTTP 400; "Insufficient balance. Available: ₫0.00" |  |
| 3 | Customer | Verify no transaction record created | History unchanged |  |

---

### UAT-019 — Admin Disable User Account

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-019 |
| Business Process | Administration — User Management |
| Actor | Bank Administrator |
| Priority | High |

| Step | Actor | Action | Expected Business Outcome | Pass/Fail |
|------|-------|--------|--------------------------|-----------|
| 1 | Bank Administrator | Disable customer account (enabled = false) | Admin confirms action; audit log created |  |
| 2 | Customer | Attempt to log in | HTTP 401; "Account is disabled" |  |
| 3 | Bank Administrator | Re-enable account | Customer can log in successfully |  |

---

### UAT-020 — Full Lifecycle Test (Regression)

| Attribute | Value |
|-----------|-------|
| UAT ID | UAT-020 |
| Business Process | End-to-End Platform Lifecycle |
| Actor | Customer, Operations Officer, Compliance Officer |
| Priority | Critical |
| Description | Execute UAT-001 through UAT-006 in sequence for a single test customer to verify the full lifecycle: Register → Verify → Login → KYC → Transfer → Freeze → Unfreeze → Review History |

| Step | Covers UAT | Pass/Fail |
|------|-----------|-----------|
| 1 | UAT-001: Registration + Email Verification | |
| 2 | UAT-005: KYC Submission and Approval | |
| 3 | UAT-002: Fund Transfer (x3 transfers) | |
| 4 | UAT-006: Account Freeze and Unfreeze | |
| 5 | UAT-002: Transfer after Unfreeze | |
| 6 | UAT-007: Compliance Officer History Review | |
| 7 | UAT-011: RBAC verification | |

---

## 9. UAT Sign-Off Template

---

```
╔══════════════════════════════════════════════════════════════════════════╗
║              BANKING CORE PLATFORM — UAT SIGN-OFF DOCUMENT              ║
╚══════════════════════════════════════════════════════════════════════════╝

Project:          Banking Core Platform
Release Version:  1.0.0
UAT Period:       [Start Date] to [End Date]
Environment:      UAT (http://uat-banking-platform.internal)
Document Ref:     BCP-UAT-SIGNOFF-001

──────────────────────────────────────────────────────────────────────────
SECTION 1 — UAT EXECUTION SUMMARY
──────────────────────────────────────────────────────────────────────────

  Total UAT Scenarios:     20
  Scenarios Executed:      [ ___ ]
  Scenarios Passed:        [ ___ ]
  Scenarios Failed:        [ ___ ]
  Scenarios Deferred:      [ ___ ]

  Critical Defects Open:   [ ___ ]
  High Defects Open:       [ ___ ]
  Medium Defects Open:     [ ___ ]

  Pass Rate:               [ ___% ]

──────────────────────────────────────────────────────────────────────────
SECTION 2 — UAT SCENARIO RESULTS SUMMARY
──────────────────────────────────────────────────────────────────────────

  UAT ID   | Description                          | Result  | Defect Ref
  ─────────┼──────────────────────────────────────┼─────────┼──────────────
  UAT-001  | Customer Onboarding                  | [ ]PASS | 
           |                                      | [ ]FAIL | 
           |                                      | [ ]SKIP |
  ─────────┼──────────────────────────────────────┼─────────┼──────────────
  UAT-002  | Fund Transfer — Happy Path           | [ ]PASS |
           |                                      | [ ]FAIL |
           |                                      | [ ]SKIP |
  ─────────┼──────────────────────────────────────┼─────────┼──────────────
  UAT-003  | Transfer — Insufficient Balance      | [ ]PASS |
  UAT-004  | Password Reset                       | [ ]PASS |
  UAT-005  | KYC Verification                     | [ ]PASS |
  UAT-006  | Account Freeze                       | [ ]PASS |
  UAT-007  | Compliance History Review            | [ ]PASS |
  UAT-008  | Google OAuth2 Login                  | [ ]PASS |
  UAT-009  | Session Expiry & Refresh             | [ ]PASS |
  UAT-010  | Duplicate Transfer Prevention        | [ ]PASS |
  UAT-011  | RBAC Enforcement                     | [ ]PASS |
  UAT-012  | Distributed Trace Visibility         | [ ]PASS |
  UAT-013  | Service Health Check                 | [ ]PASS |
  UAT-014  | Failed Transaction Visibility        | [ ]PASS |
  UAT-015  | KYC Rejection                        | [ ]PASS |
  UAT-016  | Concurrent Transfer Safety           | [ ]PASS |
  UAT-017  | Resend Verification Email            | [ ]PASS |
  UAT-018  | Zero Balance Transfer                | [ ]PASS |
  UAT-019  | Admin Disable User                   | [ ]PASS |
  UAT-020  | Full Lifecycle Regression            | [ ]PASS |
  ─────────┴──────────────────────────────────────┴─────────┴──────────────

──────────────────────────────────────────────────────────────────────────
SECTION 3 — OUTSTANDING DEFECTS
──────────────────────────────────────────────────────────────────────────

  Defect ID | Severity | Description | UAT Scenario | Status | Owner
  ──────────┼──────────┼─────────────┼──────────────┼────────┼───────
            |          |             |              |        |
            |          |             |              |        |
            |          |             |              |        |

──────────────────────────────────────────────────────────────────────────
SECTION 4 — DEFERRED ITEMS
──────────────────────────────────────────────────────────────────────────

  # | Item Description                    | Reason for Deferral | Target Release
  ──┼─────────────────────────────────────┼─────────────────────┼───────────────
    |                                     |                     |
    |                                     |                     |

──────────────────────────────────────────────────────────────────────────
SECTION 5 — SIGN-OFF DECLARATION
──────────────────────────────────────────────────────────────────────────

  We, the undersigned, confirm that User Acceptance Testing has been 
  completed for Banking Core Platform v1.0.0 and the results are 
  documented above.

  [ ] APPROVED — System is accepted for production release.
  [ ] CONDITIONALLY APPROVED — System approved with deferred items noted.
  [ ] NOT APPROVED — Outstanding critical defects prevent release.

  ┌─────────────────────────────────────────────────────────────────────┐
  │  Role                │ Name           │ Signature │ Date           │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ Product Owner        │                │           │                │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ Retail Banking Head  │                │           │                │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ IT Development Lead  │                │           │                │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ QA Lead              │                │           │                │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ Compliance Officer   │                │           │                │
  ├──────────────────────┼────────────────┼───────────┼────────────────┤
  │ Operations Officer   │                │           │                │
  └─────────────────────────────────────────────────────────────────────┘

  Comments / Conditions:
  ______________________________________________________________________
  ______________________________________________________________________
  ______________________________________________________________________

  Approved for Production Release: [ ] YES   [ ] NO

  Date of Sign-Off: ____________________

╚══════════════════════════════════════════════════════════════════════════╝
```

---

*Document End — BCP-TST-001 v1.0*
