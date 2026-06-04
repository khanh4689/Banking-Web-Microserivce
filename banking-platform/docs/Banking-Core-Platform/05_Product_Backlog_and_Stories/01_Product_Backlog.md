# Banking Core Platform
## Document 05 — Product Backlog & User Stories

**Document ID:** BCP-PB-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team / Product Owner  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [Epic Summary](#1-epic-summary)
2. [EPIC-001 Customer Registration & Onboarding](#epic-001-customer-registration--onboarding)
3. [EPIC-002 Authentication & Session Management](#epic-002-authentication--session-management)
4. [EPIC-003 Customer Profile & eKYC](#epic-003-customer-profile--ekyc)
5. [EPIC-004 Account Management](#epic-004-account-management)
6. [EPIC-005 Fund Transfer](#epic-005-fund-transfer)
7. [EPIC-006 Transaction History & Reporting](#epic-006-transaction-history--reporting)
8. [EPIC-007 Notifications](#epic-007-notifications)
9. [EPIC-008 Administration & Compliance](#epic-008-administration--compliance)

---

## 1. Epic Summary

| Epic ID | Epic Name | Priority | Stories | Sprint Target |
|---------|-----------|----------|---------|---------------|
| EPIC-001 | Customer Registration & Onboarding | Must Have | US-001 to US-005 | Sprint 1–2 |
| EPIC-002 | Authentication & Session Management | Must Have | US-006 to US-012 | Sprint 1–2 |
| EPIC-003 | Customer Profile & eKYC | Must Have | US-013 to US-017 | Sprint 3 |
| EPIC-004 | Account Management | Must Have | US-018 to US-022 | Sprint 3–4 |
| EPIC-005 | Fund Transfer | Must Have | US-023 to US-027 | Sprint 4–5 |
| EPIC-006 | Transaction History & Reporting | Must Have | US-028 to US-031 | Sprint 5 |
| EPIC-007 | Notifications | Should Have | US-032 to US-034 | Sprint 5–6 |
| EPIC-008 | Administration & Compliance | Must Have | US-035 to US-038 | Sprint 6 |

---

## EPIC-001 Customer Registration & Onboarding

**Epic Description:**  
Enable prospective customers to register for banking services digitally, without branch visits. The registration process must collect credentials, verify email ownership, and automatically provision the customer's profile and bank account.

**Business Value:**  
Reduces onboarding time from 5 days (branch-based) to under 10 minutes. Targets a 32-percentage-point reduction in customer dropout rate.

**Acceptance Criteria (Epic Level):**
- A customer with no existing account can complete registration in under 10 minutes.
- The system provisioned a verified account with KYC status PENDING.
- Email verification is required before login access is granted.

---

### US-001 — New Customer Registration

**Story:**
> As a prospective customer,  
> I want to register on the platform by providing my username, email, and password,  
> So that I can begin the process of opening a bank account without visiting a branch.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** None

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Customer Registration

  Scenario: Successful registration with valid credentials
    Given the customer is not already registered
    And the customer navigates to the registration page
    When the customer submits: username="john_doe", email="john@example.com", password="SecurePass1"
    Then the system creates a new user record in auth_db
    And the account is set to enabled=false, emailVerified=false
    And the system sends a verification email to "john@example.com"
    And the system returns HTTP 200 with message "User registered. Please verify your email."

  Scenario: Registration fails when username is already taken
    Given the username "john_doe" is already registered
    When the customer submits registration with username="john_doe"
    Then the system returns HTTP 409
    And the response body contains "Username already taken"
    And no new user record is created

  Scenario: Registration fails when email is already registered
    Given the email "john@example.com" is already registered
    When the customer submits registration with email="john@example.com"
    Then the system returns HTTP 409
    And the response body contains "Email already registered"

  Scenario: Registration fails with weak password
    When the customer submits registration with password="abc"
    Then the system returns HTTP 400
    And the response contains field-level validation error for "password"
    And no user record is created

  Scenario: Registration fails with invalid email format
    When the customer submits registration with email="not-an-email"
    Then the system returns HTTP 400
    And the response contains field-level validation error for "email"
```

---

### US-002 — Email Verification

**Story:**
> As a registered customer,  
> I want to verify my email address by clicking a link sent to my inbox,  
> So that my account is activated and I can log in to the banking platform.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-001

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Email Verification

  Scenario: Successful email verification
    Given a registered customer with emailVerified=false
    And a valid, unexpired verification token exists
    When the customer sends GET /auth/verify-email?token={validToken}
    Then the system sets emailVerified=true and enabled=true
    And the verification token is marked as used
    And the system returns HTTP 200 with message "Email verified successfully."

  Scenario: Verification fails with expired token
    Given a verification token that has passed its expiry timestamp
    When the customer sends GET /auth/verify-email?token={expiredToken}
    Then the system returns HTTP 400
    And the response contains "Invalid or expired verification token."
    And the user account remains inactive (enabled=false)

  Scenario: Verification fails with already-used token
    Given a verification token that has already been used
    When the customer sends GET /auth/verify-email?token={usedToken}
    Then the system returns HTTP 400
    And the response contains "Invalid or expired verification token."

  Scenario: Resend verification email
    Given a registered customer with emailVerified=false
    When the customer sends POST /auth/resend-verification-email with { "email": "john@example.com" }
    Then a new verification token is generated
    And the old token is invalidated
    And a new verification email is sent to "john@example.com"
    And the system returns HTTP 200
```

---

### US-003 — Automatic Profile Provisioning on Registration

**Story:**
> As the banking system,  
> I want to automatically create a customer profile when a new user registers,  
> So that the customer has a complete Customer Information File (CIF) without manual bank staff intervention.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-001, Kafka infrastructure

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Automatic Profile Provisioning

  Scenario: Profile created automatically after email verification
    Given a customer has verified their email
    And auth-service has published a UserCreatedEvent to the "user-events" Kafka topic
    When user-service consumes the UserCreatedEvent
    Then a new UserProfile record is created in user_db.users_profile
    And the profile.auth_id matches the event.id
    And the profile.email matches the event.email
    And the profile.kycStatus is set to "PENDING"
    And profile creation completes within 5 seconds of event publication

  Scenario: Duplicate event does not create duplicate profile
    Given a UserCreatedEvent with id="user-123" has already been processed
    When user-service receives the same event again (Kafka retry)
    Then no duplicate UserProfile is created
    And the existing profile is unchanged
    And the consumer logs a warning but does not throw an exception
```

---

### US-004 — Automatic Account Provisioning on Registration

**Story:**
> As the banking system,  
> I want to automatically open a bank account when a new customer registers,  
> So that the customer can receive and send funds immediately after onboarding without contacting the bank.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-001, Kafka infrastructure

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Automatic Account Provisioning

  Scenario: Account created automatically after registration event
    Given auth-service has published a UserCreatedEvent for userId="user-456"
    When account-service consumes the UserCreatedEvent
    Then a new Account record is created in account_db.accounts
    And account.userId matches the event.id
    And account.balance is 0.00
    And account.currency is "VND"
    And account.status is "ACTIVE"
    And account.accountNumber is a unique 10-digit numeric string
    And account creation completes within 5 seconds of event publication

  Scenario: Duplicate event does not create duplicate account
    Given an Account already exists for userId="user-456"
    When account-service receives another UserCreatedEvent for userId="user-456"
    Then no additional Account record is created
    And a warning is logged
    And no exception is thrown

  Scenario: Account number uniqueness guaranteed
    Given 10,000 accounts have been created
    When a new account is provisioned
    Then the generated account number is not already in the accounts table
```

---

### US-005 — Google OAuth2 Social Registration

**Story:**
> As a prospective customer,  
> I want to register and log in using my existing Google account,  
> So that I don't need to create and remember a separate password for the banking platform.

**Story Points:** 8  
**Priority:** Should Have  
**Dependencies:** US-001, Google OAuth2 client credentials

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Google OAuth2 Registration and Login

  Scenario: New user registers via Google OAuth2
    Given the customer does not have an existing account
    And the customer clicks "Sign in with Google"
    When the OAuth2 flow completes and Google returns a valid user profile
    Then a new User record is created in auth_db with the Google email
    And emailVerified is set to true (Google guarantees ownership)
    And a UserCreatedEvent is published to Kafka
    And a JWT access token and refresh token are returned
    And the customer is redirected to the platform dashboard

  Scenario: Existing Google user logs in
    Given a customer has previously logged in via Google with email="user@gmail.com"
    When the customer completes the Google OAuth2 flow with the same email
    Then no duplicate User record is created
    And a new JWT access token is issued
    And the customer is directed to their dashboard
```

---

## EPIC-002 Authentication & Session Management

**Epic Description:**  
Implement a secure, stateless authentication system using JWT with RSA RS256 signing. Support access token issuance, refresh token rotation with theft detection, and password management.

**Business Value:**  
Industry-standard token security; supports mobile and web clients without server-side sessions; enables zero-trust service-to-service auth.

---

### US-006 — Customer Login

**Story:**
> As a verified customer,  
> I want to log in with my username and password,  
> So that I receive an access token to use banking services.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-001, US-002

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Customer Login

  Scenario: Successful login with valid credentials
    Given the customer "john_doe" is registered and email-verified
    When the customer sends POST /auth/login with { "username": "john_doe", "password": "SecurePass1" }
    Then the system returns HTTP 200
    And the response body contains an accessToken (JWT)
    And the response body contains a refreshToken
    And the response body contains expiresIn (seconds)
    And the JWT sub claim equals the customer's userId
    And the JWT roles claim contains "ROLE_USER"

  Scenario: Login fails with incorrect password
    Given the customer "john_doe" exists
    When the customer sends POST /auth/login with { "username": "john_doe", "password": "WrongPass" }
    Then the system returns HTTP 401
    And the response body contains "Invalid credentials"
    And no token is issued
    And a LOGIN_FAILED log entry is written (username only, no password)

  Scenario: Login fails for unverified email
    Given the customer "jane_doe" is registered but emailVerified=false
    When the customer sends POST /auth/login with valid credentials
    Then the system returns HTTP 401
    And the response body contains "Email not verified"
```

---

### US-007 — JWT Access Token Validation

**Story:**
> As a resource service (user-service, account-service, transaction-service),  
> I want to validate incoming JWTs using the JWKS public key endpoint,  
> So that only authenticated and authorised requests are processed.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-006

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: JWT Token Validation

  Scenario: Valid JWT is accepted by resource service
    Given a customer has a valid, unexpired JWT
    When the customer sends GET /accounts/me with Authorization: Bearer {validJWT}
    Then the resource service validates the JWT signature against JWKS public key
    And the service extracts the userId from the sub claim
    And the request is processed normally

  Scenario: Expired JWT is rejected
    Given a customer has a JWT that has passed its expiry time
    When the customer sends any protected API request
    Then the service returns HTTP 401 Unauthorized
    And the response indicates the token has expired

  Scenario: Tampered JWT is rejected
    Given a JWT with a modified payload (e.g., roles changed to ROLE_ADMIN)
    When the customer sends a request with the modified JWT
    Then the service rejects the JWT (signature mismatch)
    And returns HTTP 401 Unauthorized
```

---

### US-008 — Refresh Token Rotation

**Story:**
> As an authenticated customer,  
> I want my session to remain active after my access token expires without re-entering my password,  
> So that I have a seamless banking experience during extended sessions.

**Story Points:** 8  
**Priority:** Must Have  
**Dependencies:** US-006

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Refresh Token Rotation

  Scenario: Successfully refresh an expired access token
    Given a customer has a valid refresh token
    When the customer sends POST /auth/refresh with { "refreshToken": "{validToken}" }
    Then the system returns HTTP 200
    And the response contains a new accessToken
    And the response contains a new refreshToken
    And the old refresh token is revoked in the database (revoked=true)
    And the new refresh token hash is stored in the database

  Scenario: Refresh fails with invalid token
    Given the customer submits a refresh token that does not exist in the database
    When POST /auth/refresh is called
    Then the system returns HTTP 401
    And the response contains "Invalid or expired refresh token"

  Scenario: Refresh fails with revoked token (theft detection)
    Given a refresh token has already been used and revoked
    When POST /auth/refresh is called with the revoked token
    Then the system revokes ALL active refresh tokens for that user
    And the system returns HTTP 401
    And a security alert event is logged
```

---

### US-009 — Password Reset via Email

**Story:**
> As a customer who has forgotten their password,  
> I want to reset my password via a secure link sent to my registered email,  
> So that I can regain access to my account without contacting customer support.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-001

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Password Reset

  Scenario: Request password reset for existing email
    Given a registered customer with email="john@example.com"
    When the customer sends POST /auth/forgot-password with { "email": "john@example.com" }
    Then the system generates a password reset token valid for 1 hour
    And sends a reset email to "john@example.com"
    And returns HTTP 200 with "Password reset email sent."

  Scenario: Request password reset for non-existent email (user enumeration prevention)
    Given no account exists for email="unknown@example.com"
    When POST /auth/forgot-password is called with that email
    Then the system returns HTTP 200 with "Password reset email sent." (same response)
    And no email is sent
    And no token is generated

  Scenario: Successfully reset password with valid token
    Given a valid, unexpired password reset token
    When POST /auth/reset-password is called with { "token": "{validToken}", "newPassword": "NewSecure1" }
    Then the system updates the user's password hash
    And marks the token as used
    And returns HTTP 200 "Password reset successfully."
    And the user can log in with the new password
    And the old password no longer works

  Scenario: Password reset fails with expired token
    Given an expired password reset token
    When POST /auth/reset-password is called
    Then the system returns HTTP 400
    And the password is not changed
```

---

### US-010 — Role-Based Access Control Enforcement

**Story:**
> As the banking platform,  
> I want to enforce role-based access control on all protected endpoints,  
> So that customers can only access their own data and administrative functions are restricted to authorised staff.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-006, US-007

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Role-Based Access Control

  Scenario: ROLE_USER cannot access admin endpoint
    Given a customer with ROLE_USER
    When the customer sends GET /users/admin
    Then the system returns HTTP 403 Forbidden

  Scenario: ROLE_ADMIN can access admin endpoint
    Given a user with ROLE_ADMIN
    When the user sends GET /users/admin with a valid JWT
    Then the system returns HTTP 200

  Scenario: Customer cannot access another customer's profile
    Given customer A is authenticated with userId="user-123"
    When customer A sends GET /users/user-456 (another user's ID)
    Then the system returns HTTP 403 or HTTP 404
    And customer A's profile data is not exposed

  Scenario: Internal endpoints are not accessible via API Gateway
    Given the API Gateway route configuration
    When any external client calls /internal/accounts/...
    Then the API Gateway returns HTTP 404 (route not found)
```

---

### US-011 — Request Trace ID Propagation

**Story:**
> As an operations engineer,  
> I want every API request to carry a unique trace ID across all services,  
> So that I can correlate log entries from multiple services when investigating an incident.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** None

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Distributed Trace ID

  Scenario: Trace ID is generated and propagated
    Given a request enters the system without an X-Trace-Id header
    When the request is processed by any service
    Then the service generates a UUID trace ID
    And adds it to the MDC context
    And every log entry for this request contains the same traceId

  Scenario: Incoming Trace ID is used
    Given a request arrives with header X-Trace-Id: "abc-123"
    When the service processes the request
    Then all log entries use traceId="abc-123"
    And the trace ID is preserved throughout the request lifecycle
```

---

### US-012 — Logout / Token Revocation

**Story:**
> As an authenticated customer,  
> I want to log out from the banking platform,  
> So that my session is terminated and my refresh token is invalidated.

**Story Points:** 3  
**Priority:** Should Have  
**Dependencies:** US-006, US-008

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Logout

  Scenario: Customer logs out successfully
    Given a customer has a valid refresh token
    When POST /auth/logout is called with the refresh token
    Then the refresh token is marked as revoked in the database
    And the system returns HTTP 200
    And subsequent attempts to use the refresh token return HTTP 401
```

---

## EPIC-003 Customer Profile & eKYC

**Epic Description:**  
Enable customers to manage their personal profile and allow bank staff to manage KYC verification status. The profile is the digital CIF (Customer Information File).

---

### US-013 — View Own Profile

**Story:**
> As an authenticated customer,  
> I want to view my profile information including my name, email, KYC status, and account details,  
> So that I can verify my personal information is accurate.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-003, US-006

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: View Customer Profile

  Scenario: Authenticated customer views their own profile
    Given a customer is authenticated with a valid JWT
    When the customer sends GET /users/me
    Then the system returns HTTP 200
    And the response contains: id, authId, email, username, fullName, phone, avatar, kycStatus
    And the kycStatus reflects the current verification state
    And the profile data belongs to the authenticated customer only
```

---

### US-014 — Update Own Profile

**Story:**
> As an authenticated customer,  
> I want to update my full name, phone number, and profile picture,  
> So that my profile reflects accurate contact and identification information.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-013

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Update Customer Profile

  Scenario: Customer updates profile successfully
    Given a customer is authenticated
    When PUT /users/me is called with { "fullName": "Nguyen Van A", "phone": "+84912345678" }
    Then the system updates the profile in user_db
    And returns HTTP 200 with the updated profile
    And updatedAt timestamp is refreshed

  Scenario: Customer cannot update email via profile update
    Given a customer is authenticated
    When PUT /users/me is called with { "email": "newemail@example.com" }
    Then the system either ignores the email field or returns HTTP 400
    And the email in the database remains unchanged

  Scenario: Customer cannot update kycStatus via profile update
    When PUT /users/me is called with { "kycStatus": "VERIFIED" }
    Then the system ignores or rejects the kycStatus change
    And the kycStatus in the database remains unchanged
```

---

### US-015 — KYC Status Update by Bank Staff

**Story:**
> As an Operations Officer,  
> I want to update a customer's KYC verification status after reviewing their submitted documents,  
> So that the customer can be granted full banking service access.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-013

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: KYC Status Management

  Scenario: Operations Officer approves KYC
    Given an Operations Officer is authenticated with ROLE_OPERATIONS
    And a customer profile with kycStatus="PENDING"
    When PATCH /users/{id}/kyc is called with { "status": "VERIFIED" }
    Then kycStatus is updated to "VERIFIED" in user_db
    And an audit log entry is created with actor ID and timestamp
    And the customer is notified via email

  Scenario: Operations Officer rejects KYC
    When PATCH /users/{id}/kyc is called with { "status": "REJECTED", "reason": "Document expired" }
    Then kycStatus is updated to "REJECTED"
    And the rejection reason is recorded
    And the customer is notified with the rejection reason

  Scenario: Regular customer cannot update KYC status
    Given a customer with ROLE_USER
    When PATCH /users/{id}/kyc is called by the customer
    Then the system returns HTTP 403 Forbidden
```

---

### US-016 — KYC Document Submission

**Story:**
> As a customer with kycStatus=PENDING,  
> I want to upload my national ID and selfie for identity verification,  
> So that I can complete KYC and unlock full transaction capabilities.

**Story Points:** 8  
**Priority:** Should Have (Phase 2 for full biometric verification)  
**Dependencies:** US-013

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: KYC Document Submission

  Scenario: Customer submits KYC documents
    Given a customer with kycStatus="PENDING"
    When the customer uploads national ID (front + back) and selfie
    Then the documents are stored securely
    And kycStatus is updated to "IN_REVIEW"
    And an Operations Officer is notified of the pending review
    And the customer receives confirmation that documents are under review

  Scenario: Submission rejected due to invalid document format
    When the customer uploads a file that is not JPG or PDF
    Then the system returns HTTP 400
    And the document is not stored
    And kycStatus remains "PENDING"
```

---

### US-017 — Admin View All User Profiles

**Story:**
> As a Bank Administrator,  
> I want to view and search all customer profiles,  
> So that I can manage KYC queues and respond to compliance requirements efficiently.

**Story Points:** 3  
**Priority:** Should Have  
**Dependencies:** US-013, US-010

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Admin User Profile Management

  Scenario: Admin retrieves a specific user profile
    Given an admin is authenticated with ROLE_ADMIN
    When GET /users/{id} is called with a valid customer ID
    Then the system returns the full profile including kycStatus

  Scenario: Non-admin cannot access other users' profiles
    Given a customer with ROLE_USER
    When GET /users/{anotherUserId} is called
    Then the system returns HTTP 403
```

---

## EPIC-004 Account Management

**Epic Description:**  
Manage the lifecycle of customer bank accounts. Accounts are automatically provisioned on registration and can be manually created, queried, and status-managed.

---

### US-018 — View Own Bank Account

**Story:**
> As an authenticated customer,  
> I want to view my account details including balance, account number, and status,  
> So that I can monitor my financial position in real time.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-004, US-006

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Account Enquiry

  Scenario: Customer views their account
    Given a customer has an ACTIVE account
    And the customer is authenticated
    When GET /accounts/me is called
    Then the system returns HTTP 200
    And the response contains: id, userId, accountNumber, balance, currency, status, createdAt
    And balance is displayed with 2 decimal places
    And currency is "VND"

  Scenario: Customer with no account receives 404
    Given a customer exists but no account has been provisioned
    When GET /accounts/me is called
    Then the system returns HTTP 404 with "Account not found"
```

---

### US-019 — Manual Account Creation

**Story:**
> As an authenticated customer,  
> I want to be able to manually trigger account creation if automatic provisioning failed,  
> So that I can access banking services without contacting support.

**Story Points:** 3  
**Priority:** Should Have  
**Dependencies:** US-006

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Manual Account Creation

  Scenario: Customer creates account via API
    Given a customer is authenticated and has no existing account
    When POST /accounts is called
    Then a new account is created with balance=0, currency=VND, status=ACTIVE
    And the response contains the account details
    And HTTP 200 is returned

  Scenario: Account creation is idempotent
    Given a customer already has an account
    When POST /accounts is called again
    Then the existing account is returned (not a new one)
    And HTTP 200 is returned
    And no duplicate account is created
```

---

### US-020 — Account Status Management (Operations)

**Story:**
> As an Operations Officer,  
> I want to suspend or close a customer's account with a documented reason,  
> So that the bank can take appropriate action in response to fraud, AML triggers, or customer requests.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-018

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Account Status Management

  Scenario: Operations Officer suspends an account
    Given an account with status="ACTIVE"
    When PATCH /accounts/{accountNumber}/status is called with { "status": "SUSPENDED", "reason": "FRAUD" }
    Then account status is updated to "SUSPENDED"
    And an audit log entry is created
    And subsequent debit operations on the account return HTTP 400

  Scenario: Operations Officer reinstates a suspended account
    Given an account with status="SUSPENDED"
    When PATCH /accounts/{accountNumber}/status is called with { "status": "ACTIVE" }
    Then account status is updated to "ACTIVE"
    And the customer can initiate transfers again

  Scenario: Closed account cannot be reactivated
    Given an account with status="CLOSED"
    When PATCH /accounts/{accountNumber}/status is called with { "status": "ACTIVE" }
    Then the system returns HTTP 400
    And the account remains CLOSED
```

---

### US-021 — Account Balance Enquiry by Account Number

**Story:**
> As an internal service (transaction-service),  
> I want to query an account's balance and status by account number or userId,  
> So that I can validate transfer eligibility before executing a debit.

**Story Points:** 3  
**Priority:** Must Have (Internal)  
**Dependencies:** US-018

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Internal Account Lookup

  Scenario: Retrieve account by account number
    Given an account exists with accountNumber="1234567890"
    When GET /internal/accounts/1234567890 is called (from transaction-service)
    Then HTTP 200 is returned with full account details
    And the response contains status, balance, and currency

  Scenario: Retrieve account by userId
    Given an account exists for userId="user-123"
    When GET /internal/accounts/user/user-123 is called
    Then HTTP 200 is returned with the account details

  Scenario: Account not found
    When GET /internal/accounts/0000000000 is called
    Then HTTP 404 is returned
```

---

### US-022 — Balance Debit and Credit Operations

**Story:**
> As the transaction-service,  
> I want to atomically debit and credit account balances with pessimistic locking,  
> So that concurrent transfers do not result in incorrect or negative balances.

**Story Points:** 8  
**Priority:** Must Have  
**Dependencies:** US-021

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Account Balance Operations

  Scenario: Successful debit
    Given an ACTIVE account with balance=500000.00
    When POST /internal/accounts/{acct}/debit is called with { "amount": 100000.00 }
    Then the balance is updated to 400000.00
    And HTTP 200 is returned

  Scenario: Debit fails on insufficient balance
    Given an ACTIVE account with balance=50000.00
    When POST /internal/accounts/{acct}/debit is called with { "amount": 100000.00 }
    Then HTTP 400 is returned with "Insufficient balance"
    And the balance remains 50000.00

  Scenario: Concurrent debit uses pessimistic locking
    Given two concurrent debit requests for the same account
    When both requests arrive simultaneously
    Then one request acquires the lock and processes first
    And the second request waits and processes using the updated balance
    And no lost update occurs

  Scenario: Debit fails on SUSPENDED account
    Given an account with status="SUSPENDED"
    When a debit is attempted
    Then HTTP 400 is returned with "Account is not ACTIVE"
    And the balance is unchanged
```

---

## EPIC-005 Fund Transfer

**Epic Description:**  
Enable customers to transfer money between accounts in real time, with idempotency controls, strict balance validation, and a complete transaction audit trail.

---

### US-023 — Initiate Fund Transfer

**Story:**
> As an authenticated customer,  
> I want to transfer money to another account by entering the account number and amount,  
> So that I can pay others or settle obligations instantly from my mobile or web browser.

**Story Points:** 13  
**Priority:** Must Have  
**Dependencies:** US-006, US-022

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Fund Transfer

  Scenario: Successful fund transfer
    Given customer A has balance=1000000.00 VND
    And account B exists with status=ACTIVE
    When POST /transactions/transfer is called with { "toAccountNumber": "B", "amount": 200000.00 }
    Then HTTP 200 is returned
    And the transaction status is "SUCCESS"
    And customer A's balance is 800000.00
    And account B's balance has increased by 200000.00
    And a transaction record is persisted

  Scenario: Transfer rejected for insufficient balance
    Given customer A has balance=100000.00 VND
    When transfer amount is 200000.00
    Then HTTP 400 is returned with "Insufficient balance. Available: 100000.00"
    And no balances are changed
    And no transaction record is created

  Scenario: Transfer rejected for self-transfer
    When the toAccountNumber equals the customer's own account number
    Then HTTP 400 is returned with "Cannot transfer to the same account"

  Scenario: Transfer rejected if destination is SUSPENDED
    Given the destination account has status="SUSPENDED"
    When the transfer is attempted
    Then HTTP 400 is returned with "Destination account is not ACTIVE"
```

---

### US-024 — Idempotent Transfer Processing

**Story:**
> As a mobile banking application,  
> I want to include an Idempotency-Key header in transfer requests,  
> So that network retries do not result in duplicate charges to the customer.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Idempotent Transfers

  Scenario: Retry with same idempotency key returns original result
    Given a successful transfer with Idempotency-Key: "key-abc-123"
    When POST /transactions/transfer is called again with the same key
    Then the original transaction record is returned
    And HTTP 200 is returned
    And no additional balance changes occur
    And only one transaction record exists for that key

  Scenario: Different idempotency key creates a new transaction
    Given a transfer was made with Idempotency-Key: "key-1"
    When POST /transactions/transfer is called with Idempotency-Key: "key-2" and same details
    Then a new independent transaction is created
    And both transactions appear in the history
```

---

### US-025 — Transfer Amount Validation

**Story:**
> As the banking platform,  
> I want to enforce minimum transfer amounts and reject invalid amounts,  
> So that the system maintains data integrity and prevents processing errors.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transfer Amount Validation

  Scenario: Transfer amount below minimum is rejected
    When POST /transactions/transfer is called with { "amount": 500 }
    Then HTTP 400 is returned with "Minimum transfer amount is VND 1,000"

  Scenario: Negative amount is rejected
    When POST /transactions/transfer is called with { "amount": -100000 }
    Then HTTP 400 is returned with field validation error

  Scenario: Zero amount is rejected
    When POST /transactions/transfer is called with { "amount": 0 }
    Then HTTP 400 is returned with field validation error

  Scenario: Amount is rounded to 2 decimal places
    When POST /transactions/transfer is called with { "amount": 100000.999 }
    Then the system rounds to 101001.00 (HALF_UP rounding)
    And the transaction proceeds with the rounded amount
```

---

### US-026 — Transaction State Machine

**Story:**
> As the banking system,  
> I want every transfer to follow a defined state machine (PENDING → SUCCESS/FAILED),  
> So that there is always a clear record of what happened, even during partial failures.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transaction State Machine

  Scenario: Transaction transitions PENDING → SUCCESS
    Given a transfer is initiated
    When both debit and credit complete successfully
    Then the transaction status transitions from PENDING to SUCCESS
    And updatedAt is refreshed

  Scenario: Transaction transitions PENDING → FAILED when debit error occurs
    Given a transfer is initiated
    When the debit call to account-service fails
    Then the transaction status is updated to FAILED
    And failureReason is populated with the error description
    And HTTP 500 is returned to the client with the failure reason

  Scenario: FAILED transaction is not retryable without new idempotency key
    Given a FAILED transaction with idempotency-key="key-xyz"
    When POST /transactions/transfer is called again with the same key
    Then the FAILED transaction is returned as-is (idempotent)
    And no new transfer is attempted
```

---

### US-027 — Account-Service Resilience During Transfer

**Story:**
> As the banking platform,  
> I want the transfer process to fail gracefully when account-service is unavailable,  
> So that customers receive a meaningful error message and can safely retry.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transfer Resilience

  Scenario: Transfer fails gracefully when account-service is down
    Given the account-service is not responding
    When POST /transactions/transfer is called
    Then HTTP 503 is returned with "Account service is currently unavailable. Please try again later."
    And no transaction record in PENDING state is left open
    And the client can safely retry with the same idempotency key

  Scenario: Transfer error is logged with correlationId
    Given a transfer fails due to service unavailability
    Then a log entry is written with log level ERROR
    And the log entry contains the correlationId / idempotency key
    And the log entry identifies which service call failed
```

---

## EPIC-006 Transaction History & Reporting

**Epic Description:**  
Provide customers and bank staff with paginated, sortable transaction history for account reconciliation and compliance review.

---

### US-028 — View Transaction History

**Story:**
> As an authenticated customer,  
> I want to view a paginated list of my transactions sorted by most recent first,  
> So that I can review my spending, confirm transfers, and identify any unexpected activity.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transaction History

  Scenario: Customer retrieves transaction history (first page)
    Given the customer has 25 transactions
    When GET /transactions/history?accountNumber={acct}&page=0&size=10 is called
    Then HTTP 200 is returned
    And the content array contains 10 transactions
    And transactions are sorted by createdAt descending (newest first)
    And the response metadata shows: page=0, size=10, totalElements=25, totalPages=3

  Scenario: History includes both sent and received transactions
    Given the customer sent 5 transfers and received 3 transfers
    When the history is retrieved
    Then all 8 transactions appear in the results
    And each entry shows fromAccount and toAccount

  Scenario: History page out of range returns empty
    When GET /transactions/history?page=100 is called
    Then HTTP 200 is returned with empty content array
```

---

### US-029 — Transaction Detail View

**Story:**
> As a customer,  
> I want to view the full details of a specific transaction,  
> So that I can verify the exact amount, timing, and status of a transfer.

**Story Points:** 3  
**Priority:** Should Have  
**Dependencies:** US-028

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transaction Detail

  Scenario: Customer views a specific transaction
    Given a transaction exists with id="tx-uuid-123"
    When GET /transactions/{id} is called by the owning customer
    Then HTTP 200 is returned
    And the response contains: id, fromAccount, toAccount, amount, currency, status, failureReason, createdAt, updatedAt

  Scenario: Customer cannot view another customer's transaction
    When GET /transactions/{id} is called for a transaction not involving the customer's account
    Then HTTP 403 or HTTP 404 is returned
```

---

### US-030 — Failed Transaction Visibility

**Story:**
> As a customer,  
> I want to see failed transactions in my history with a reason,  
> So that I understand why a transfer did not complete and can take corrective action.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-026, US-028

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Failed Transaction Display

  Scenario: Failed transaction is visible in history with reason
    Given a transaction exists with status="FAILED" and failureReason="Insufficient balance"
    When the transaction history is retrieved
    Then the FAILED transaction appears in the list
    And the failureReason field is populated with "Insufficient balance"
    And the UI displays the failure reason to the customer
```

---

### US-031 — Transaction History for Compliance Review

**Story:**
> As a Compliance Officer,  
> I want to retrieve the full transaction history for any account by account number,  
> So that I can perform AML transaction monitoring and regulatory reporting.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-028, US-010

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Compliance Transaction Review

  Scenario: Compliance Officer retrieves full account history
    Given a Compliance Officer is authenticated with ROLE_COMPLIANCE
    When GET /transactions/history?accountNumber={any_acct}&page=0&size=100 is called
    Then the full transaction history is returned
    And results include all statuses: PENDING, SUCCESS, FAILED
    And the response is paginated for large datasets
```

---

## EPIC-007 Notifications

**Epic Description:**  
Deliver automated email notifications to customers for key platform events including registration, KYC updates, and transfer confirmations.

---

### US-032 — Registration Confirmation Email

**Story:**
> As a new customer,  
> I want to receive a confirmation email when I register,  
> So that I know my registration was received and I have the verification link.

**Story Points:** 2  
**Priority:** Must Have  
**Dependencies:** US-001

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Registration Email

  Scenario: Verification email sent on registration
    Given a new customer has submitted the registration form
    When the auth-service creates the user record
    Then a verification email is sent to the registered email address
    And the email contains a clickable verification link with the token
    And the email is delivered within 2 minutes
    And the email sender is the bank's configured no-reply address
```

---

### US-033 — Transfer Confirmation Notification

**Story:**
> As a customer,  
> I want to receive an email notification when a transfer is completed from my account,  
> So that I am immediately aware of money leaving my account.

**Story Points:** 5  
**Priority:** Should Have  
**Dependencies:** US-023

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Transfer Notification

  Scenario: Customer receives email after successful transfer
    Given a transfer from account A to account B completes with status=SUCCESS
    When the transaction-service marks the transaction as SUCCESS
    Then an email is sent to the owner of account A
    And the email includes: amount, destination account, transaction reference, timestamp
    And the email is delivered within 5 minutes of transaction completion

  Scenario: No notification for FAILED transfers
    Given a transaction has status=FAILED
    Then no success notification is sent
    And optionally a failure notification may be sent (configurable)
```

---

### US-034 — Account Freeze Notification

**Story:**
> As a customer whose account has been suspended,  
> I want to receive a notification informing me of the restriction,  
> So that I can contact the bank for resolution.

**Story Points:** 3  
**Priority:** Must Have  
**Dependencies:** US-020

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Account Freeze Notification

  Scenario: Customer notified when account is suspended
    Given an account is suspended by an Operations Officer
    When the account status is updated to SUSPENDED
    Then an email is sent to the account owner
    And the email instructs the customer to contact the bank
    And the email does NOT disclose AML investigation details (regulatory requirement)
    And the email includes a reference number for the customer to quote
```

---

## EPIC-008 Administration & Compliance

**Epic Description:**  
Provide bank staff with administrative tools for user management, account oversight, audit log review, and compliance functions.

---

### US-035 — Audit Log Review

**Story:**
> As a Compliance Officer,  
> I want to view an audit trail of all significant platform actions,  
> So that I can demonstrate regulatory compliance and investigate incidents.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** All previous epics

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Audit Log Access

  Scenario: Compliance Officer retrieves audit trail for a user
    Given a Compliance Officer is authenticated
    When GET /audit/logs?userId={id}&from={date}&to={date} is called
    Then all audit events for the specified user and date range are returned
    And each event includes: eventType, actorId, targetId, timestamp, details

  Scenario: Audit logs are immutable
    Given an audit log entry has been created
    When any attempt is made to update or delete the entry
    Then the operation is rejected (HTTP 403 or 405)
    And the log entry remains unchanged
```

---

### US-036 — Admin User Search and Management

**Story:**
> As a Bank Administrator,  
> I want to search for customer accounts by username or email,  
> So that I can resolve customer queries and manage the user base efficiently.

**Story Points:** 3  
**Priority:** Should Have  
**Dependencies:** US-017

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Admin User Management

  Scenario: Admin searches by username
    Given a Bank Administrator is authenticated with ROLE_ADMIN
    When GET /users?username=john_doe is called
    Then matching user profiles are returned
    And results are paginated

  Scenario: Admin disables a user account
    When PATCH /auth/users/{id}/disable is called by an admin
    Then the user's enabled flag is set to false
    And the user cannot log in
    And an audit log entry is created
```

---

### US-037 — System Health Monitoring

**Story:**
> As an IT Operations Engineer,  
> I want each service to expose a health endpoint,  
> So that monitoring tools and Docker orchestration can determine service readiness.

**Story Points:** 2  
**Priority:** Must Have  
**Dependencies:** None

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: Service Health Endpoints

  Scenario: Healthy service returns 200
    Given all required dependencies (DB, Kafka) are available
    When GET /actuator/health is called on any service
    Then HTTP 200 is returned with { "status": "UP" }
    And no authentication is required

  Scenario: Degraded service reflects in health status
    Given the database connection is unavailable
    When GET /actuator/health is called
    Then HTTP 503 or a DOWN status is returned
    And dependent service health checks fail accordingly
```

---

### US-038 — CI/CD Pipeline for All Services

**Story:**
> As a Development Team Lead,  
> I want automated CI/CD pipelines for all microservices,  
> So that code quality is enforced and deployments are consistent and repeatable.

**Story Points:** 5  
**Priority:** Must Have  
**Dependencies:** All services

**Acceptance Criteria (Gherkin):**

```gherkin
Feature: CI/CD Pipeline

  Scenario: Pipeline triggers on pull request to main
    Given code is pushed to a feature branch and a PR is opened to main
    When the CI pipeline is triggered
    Then all four services are built in parallel (matrix strategy)
    And unit tests are executed for each service
    And the pipeline fails if any service fails to build or any test fails
    And a Docker image is built per service on success

  Scenario: Pipeline fails on test failure
    Given a unit test fails in account-service
    When the CI pipeline runs
    Then the account-service job is marked as failed
    And the PR cannot be merged until the failure is resolved
```

---

*Document End — BCP-PB-001 v1.0*
