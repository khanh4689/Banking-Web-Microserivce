# Banking Core Platform
## Document 03 — BPMN Process Flows

**Document ID:** BCP-BPF-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [PF-001 Customer Registration](#pf-001-customer-registration)
2. [PF-002 eKYC Verification](#pf-002-ekyc-verification)
3. [PF-003 Login Authentication](#pf-003-login-authentication)
4. [PF-004 Open Bank Account](#pf-004-open-bank-account)
5. [PF-005 Add Beneficiary](#pf-005-add-beneficiary)
6. [PF-006 Transfer Money](#pf-006-transfer-money)
7. [PF-007 View Transaction History](#pf-007-view-transaction-history)
8. [PF-008 Account Freeze](#pf-008-account-freeze)

---

## PF-001 Customer Registration

### Overview
The Customer Registration process enables a prospective customer to digitally enrol on the Banking Core Platform without visiting a branch. The process spans the auth-service, Kafka event bus, user-service, and account-service.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Initiates registration via web/mobile |
| Auth Service | Validates credentials, persists user, publishes event |
| Email Service (SMTP) | Delivers verification email |
| Kafka (user-events topic) | Propagates registration event to downstream services |
| User Service | Consumes event; creates customer profile |
| Account Service | Consumes event; provisions bank account |

### Preconditions
- The customer does not have an existing account with the bank.
- The customer has access to the submitted email address.
- The platform's auth-service, Kafka broker, user-service, and account-service are all operational.
- Email relay (SMTP) service is available.

### Main Flow

```
START
│
├─[1] Customer submits registration form
│     Input: { username, email, password }
│     Channel: POST /auth/register
│
├─[2] auth-service validates input
│     • Username format: alphanumeric, 3–50 chars
│     • Email format: RFC 5322 compliance
│     • Password complexity: min 8 chars, mix of upper/lower/digit
│
├─[3] auth-service checks uniqueness
│     • Query: username NOT EXISTS in users table
│     • Query: email NOT EXISTS in users table
│
├─[4] auth-service creates User record
│     • Hash password with BCrypt (work factor 10)
│     • Set enabled = false, emailVerified = false
│     • Assign ROLE_USER
│     • Persist to auth_db.users
│
├─[5] auth-service generates email verification token
│     • Create UUID token
│     • Set expiry: NOW + 24 hours
│     • Persist token to auth_db.email_verification_tokens
│
├─[6] auth-service sends verification email
│     • SMTP relay: verification link with token
│     • Email template: "Please verify your email"
│
├─[7] auth-service returns HTTP 200
│     Response: "User registered. Please verify your email."
│
├─[8] Customer receives email and clicks verification link
│     Channel: GET /auth/verify-email?token={token}
│
├─[9] auth-service validates verification token
│     • Token exists in DB
│     • Token not expired
│     • Token not already used
│
├─[10] auth-service activates user account
│      • Set emailVerified = true
│      • Set enabled = true
│      • Mark token as used
│
├─[11] auth-service publishes UserCreatedEvent to Kafka
│      • @TransactionalEventListener(AFTER_COMMIT)
│      • Topic: user-events
│      • Payload: { id, email, username }
│
├─[12A] user-service consumes UserCreatedEvent
│       Consumer group: user-service-group
│       • Create UserProfile record
│       • Set kycStatus = PENDING
│       • Persist to user_db.users_profile
│
├─[12B] account-service consumes UserCreatedEvent (parallel)
│       Consumer group: account-service-group
│       • Generate unique 10-digit account number
│       • Create Account record: balance=0, currency=VND, status=ACTIVE
│       • Persist to account_db.accounts
│
└─[13] Registration complete
        Customer can now log in and access banking services
```

### Alternative Flows

**AF-001 — Username Already Exists**
- Step 3 detects username conflict.
- auth-service returns HTTP 409 Conflict: "Username already taken."
- Process ends. Customer must choose a different username.

**AF-002 — Email Already Registered**
- Step 3 detects email conflict.
- auth-service returns HTTP 409 Conflict: "Email already registered."
- Process ends. Customer is advised to use the "forgot password" flow.

**AF-003 — Password Fails Complexity Check**
- Step 2 validation fails password rules.
- auth-service returns HTTP 400 Bad Request with field-level error message.
- Process ends. Customer corrects password and resubmits.

**AF-004 — Verification Token Expired**
- Step 9: token exists but expiry timestamp has passed.
- auth-service returns HTTP 400: "Verification token has expired."
- Customer uses "Resend Verification Email" (POST /auth/resend-verification-email).
- A new token is generated and emailed. Original token is invalidated.

**AF-005 — SMTP Email Delivery Failure**
- Step 6: SMTP relay unavailable.
- auth-service logs error; user record is created but email not delivered.
- Customer can request resend via POST /auth/resend-verification-email.
- Operations team is alerted to SMTP failure.

**AF-006 — Kafka Consumer Failure (Account/Profile Provisioning)**
- Steps 12A or 12B fail (DB unavailable, etc.).
- Consumer throws exception, triggering Kafka retry mechanism.
- Operations alert fires if consumer lag exceeds threshold.
- Account/profile creation is retried until successful.

### Post Conditions
- User record exists in auth_db with emailVerified = true, enabled = true.
- UserProfile record exists in user_db with kycStatus = PENDING.
- Account record exists in account_db with status = ACTIVE, balance = 0.00 VND.
- CustomerCreated event is in Kafka topic event log.
- Customer can successfully authenticate.

---

## PF-002 eKYC Verification

### Overview
The eKYC Verification process transitions a customer's KYC status from PENDING to VERIFIED (or REJECTED), enabling full transaction capabilities. In Phase 1, this is managed by bank staff via admin functions. Phase 2 integrates with a third-party eKYC vendor API.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Submits KYC documents via platform |
| Bank Teller / Operations Officer | Reviews KYC submission and approves/rejects |
| Compliance Officer | Oversees KYC quality and regulatory adherence |
| user-service | Stores KYC status; processes updates |

### Preconditions
- Customer account is registered and email is verified.
- Customer has logged in and submitted KYC documentation through the profile interface.
- An authorised bank staff member is reviewing the KYC queue.

### Main Flow

```
START
│
├─[1] Customer logs into platform
│
├─[2] System detects kycStatus = PENDING
│     Dashboard displays KYC verification prompt
│
├─[3] Customer submits KYC documents
│     Documents: National ID / Passport (front + back), selfie
│     Channel: PUT /users/me (with document references)
│     kycStatus updated to: IN_REVIEW
│
├─[4] Operations Officer receives KYC review task
│     KYC queue in back-office system
│     Actor: Operations Officer
│
├─[5] Operations Officer reviews submitted documents
│     • Verify name matches ID document
│     • Verify date of birth
│     • Check document expiry (must be valid)
│     • Verify selfie matches ID photo (liveness check in Phase 2)
│     • Check national ID / passport number format
│
├─[6] Operations Officer makes decision
│     ├─[APPROVE]─► Step 7
│     └─[REJECT]──► Step 9
│
├─[7] Operations Officer approves KYC
│     • PATCH /users/{id}/kyc with { "status": "VERIFIED" }
│     • user-service updates kycStatus = VERIFIED
│     • Audit log entry created: actor, timestamp, user affected
│
├─[8] System notifies customer of approval
│     Email: "Your KYC verification is complete. 
│              Full banking services are now available."
│     Process ends — SUCCESS
│
├─[9] Operations Officer rejects KYC
│     • PATCH /users/{id}/kyc with { "status": "REJECTED", "reason": "..." }
│     • user-service updates kycStatus = REJECTED
│     • Audit log entry created
│
└─[10] System notifies customer of rejection
        Email: "KYC verification unsuccessful. 
                Please resubmit with valid documentation."
        Customer may resubmit — returns to Step 3
```

### Alternative Flows

**AF-001 — Document Quality Insufficient**
- Step 5: Documents are blurry or incomplete.
- Operations Officer sets status to PENDING with review note.
- Customer is notified to resubmit clearer images.

**AF-002 — Identity Mismatch**
- Step 5: Name or date of birth on ID does not match customer record.
- Submission is escalated to Compliance Officer.
- Compliance Officer may request additional documentation.

**AF-003 — Sanctions / PEP Hit**
- Step 5: Name matches a sanctions list or PEP database entry.
- Submission is immediately escalated to Compliance Officer.
- Account remains SUSPENDED pending compliance decision.
- Mandatory STR may be generated.

### Post Conditions
- Customer kycStatus is VERIFIED or REJECTED in user_db.
- Audit record exists for the KYC decision with actor ID and timestamp.
- Customer notified via email of outcome.
- If VERIFIED: customer has access to full transaction limits.

---

## PF-003 Login Authentication

### Overview
The Login Authentication process validates customer credentials and issues a JWT access token and refresh token to enable access to protected banking services.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Submits login credentials |
| auth-service | Validates credentials; issues JWT and refresh token |
| API Gateway | Routes request to auth-service |

### Preconditions
- Customer has a registered account with emailVerified = true and enabled = true.
- Customer knows their username and password.
- auth-service is operational; PostgreSQL auth_db is accessible.

### Main Flow

```
START
│
├─[1] Customer submits login form
│     Input: { username, password }
│     Channel: POST /auth/login
│
├─[2] API Gateway routes request to auth-service
│
├─[3] auth-service invokes AuthenticationManager
│     • Loads user from auth_db by username
│     • BCrypt.matches(submittedPassword, storedHash)
│
├─[4] auth-service checks account status
│     • emailVerified == true?
│     • enabled == true?
│
├─[5] auth-service generates JWT access token
│     • JwtUtil.generateToken(userId, username, roles)
│     • Algorithm: RSA RS256
│     • Claims: sub=userId, username, roles, iat, exp
│     • Expiry: 10 minutes
│
├─[6] auth-service creates refresh token
│     • Generate cryptographically random UUID string
│     • SHA-256 hash the token for DB storage
│     • Persist hashed token to auth_db.refresh_tokens
│       (with expiry = NOW + 7 days)
│
├─[7] auth-service returns token pair
│     HTTP 200
│     Body: { accessToken, refreshToken, expiresIn }
│
└─[8] Customer's client stores tokens
        accessToken: in-memory (short-lived)
        refreshToken: secure HTTP-only cookie or encrypted storage
        Process complete — customer is authenticated
```

### Alternative Flows

**AF-001 — Invalid Credentials**
- Step 3: BCrypt comparison fails.
- auth-service returns HTTP 401: "Invalid credentials."
- Log entry: `LOGIN_FAILED - username: {username}` (no password detail logged).

**AF-002 — Email Not Verified**
- Step 4: emailVerified = false.
- auth-service returns HTTP 401: "Email not verified."
- Customer is advised to check their email or use resend.

**AF-003 — Account Disabled**
- Step 4: enabled = false (administratively disabled).
- auth-service returns HTTP 401: "Account is disabled."
- Customer is advised to contact customer service.

**AF-004 — Access Token Expired (Token Refresh Sub-Flow)**
- Customer makes API call; receives HTTP 401 (token expired).
- Client submits POST /auth/refresh with refresh token.
- auth-service validates refresh token hash against DB.
- New access token + new refresh token issued (old refresh token revoked).
- Client retries original request with new access token.

**AF-005 — Google OAuth2 Login**
- Customer selects "Sign in with Google."
- Browser redirects to Google OAuth2 authorisation page.
- Customer authorises; Google redirects with authorisation code.
- auth-service exchanges code for Google user profile.
- If new user: UserCreatedEvent published; profile + account provisioned.
- auth-service issues JWT and redirect to platform dashboard.

### Post Conditions
- Customer receives valid JWT access token (exp: 10 min).
- Customer receives valid refresh token (exp: 7 days) stored in DB as hash.
- Log entry: `LOGIN_SUCCESS - userId: {uuid}`.
- Customer can access all protected API endpoints.

---

## PF-004 Open Bank Account

### Overview
Bank account provisioning occurs automatically as part of the registration event pipeline. This process describes both the automatic provisioning (triggered by Kafka event) and the manual creation path via the account API.

### Actors
| Actor | Role in Process |
|-------|----------------|
| account-service | Receives event or API call; creates account |
| Kafka (user-events) | Delivers UserCreatedEvent to account-service |
| Customer (direct API) | Can POST /accounts to create account manually if not yet provisioned |
| User (JWT) | Provides authentication context |

### Preconditions
- Customer registration is complete and email is verified.
- Kafka UserCreatedEvent has been published by auth-service.
- account-service and account_db are operational.

### Main Flow (Event-Driven — Primary Path)

```
START
│
├─[1] auth-service publishes UserCreatedEvent after COMMIT
│     Topic: user-events
│     Payload: { id: userId, email, username }
│
├─[2] account-service KafkaListener receives event
│     Consumer group: account-service-group
│
├─[3] account-service checks for existing account
│     SELECT * FROM accounts WHERE user_id = ?
│     ├─ EXISTS → Return existing account (idempotent skip)
│     └─ NOT EXISTS → Proceed to Step 4
│
├─[4] account-service generates unique account number
│     • Random 10-digit number (1,000,000,000 to 9,999,999,999)
│     • Check uniqueness in DB
│     • Regenerate if collision detected (rare; DB constraint backstop)
│
├─[5] account-service creates Account record
│     • id: UUID
│     • userId: from event
│     • accountNumber: generated 10-digit
│     • balance: 0.00
│     • currency: VND
│     • status: ACTIVE
│     • version: 0 (optimistic lock seed)
│     • createdAt: NOW
│
├─[6] Account persisted to account_db.accounts
│
└─[7] Log: ACCOUNT_CREATE - userId, accountNumber
        Account provisioning complete
```

### Alternative Flows

**AF-001 — Manual Account Creation via API**
- Customer calls POST /accounts (JWT authenticated).
- account-service extracts userId from JWT subject claim.
- Follows steps 3–7 above.
- Returns AccountResponse with account details.

**AF-002 — Duplicate Event Received (Kafka Retry)**
- Step 3 finds existing account for userId.
- Service logs warning: "Account already exists for userId. Returning existing."
- Returns existing AccountResponse without creating duplicate.

**AF-003 — Database Unavailable During Provisioning**
- Step 6 fails due to DB connection error.
- Consumer throws exception; Kafka retries delivery per retry policy.
- Alert fired to operations team on consumer lag threshold breach.
- Account created on successful retry.

### Post Conditions
- Account record exists: { userId, accountNumber, balance=0, currency=VND, status=ACTIVE }.
- Account is accessible via GET /accounts/me.
- Account can receive fund transfers immediately.

---

## PF-005 Add Beneficiary

> **Note:** Phase 1 of the Banking Core Platform supports own-bank transfers (both source and destination accounts are on this platform). Full beneficiary management with inter-bank accounts is deferred to Phase 2. The following process describes the Phase 2 design for reference.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Initiates beneficiary addition |
| Beneficiary Service (Phase 2) | Validates and stores beneficiary details |
| Interbank Directory (Phase 2) | Validates destination account at target bank |

### Preconditions
- Customer is authenticated (valid JWT).
- Customer has an ACTIVE account.
- Destination bank is a participant in the interbank directory.

### Main Flow

```
START
│
├─[1] Customer navigates to Beneficiary Management screen
│
├─[2] Customer enters beneficiary details
│     Input: { beneficiaryName, accountNumber, bankCode, alias }
│
├─[3] System validates account number format
│     • Length and format check per destination bank's specification
│
├─[4] System calls Interbank Directory API
│     • Validates account number exists at destination bank
│     • Retrieves account holder name for confirmation
│
├─[5] System displays account holder name to customer
│     "Confirm beneficiary: NGUYEN VAN A at VIETCOMBANK"
│
├─[6] Customer confirms beneficiary details
│
├─[7] System creates Beneficiary record
│     • id: UUID
│     • customerId: from JWT
│     • beneficiaryName: confirmed name from bank
│     • accountNumber: validated account
│     • bankCode: destination bank identifier
│     • alias: customer-defined nickname
│     • status: ACTIVE
│     • createdAt: NOW
│
└─[8] Beneficiary available for transfer selection
```

### Alternative Flows

**AF-001 — Account Number Not Found at Destination Bank**
- Step 4: Interbank directory returns NOT FOUND.
- System displays: "Account number not found. Please verify details."
- Process ends. Customer corrects and resubmits.

**AF-002 — Duplicate Beneficiary**
- Step 7: Same account number already exists in customer's beneficiary list.
- System displays: "This beneficiary already exists in your list."
- Customer may update the alias for the existing record.

### Post Conditions
- Beneficiary record stored and linked to customer's CIF.
- Beneficiary available in transfer beneficiary selection dropdown.
- Audit log entry created.

---

## PF-006 Transfer Money

### Overview
The Transfer Money process executes a real-time domestic fund transfer between two accounts on the platform. It implements a PENDING → SUCCESS/FAILED state machine with pessimistic locking to ensure balance integrity.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Initiates transfer via mobile/web |
| API Gateway | Routes authenticated request to transaction-service |
| transaction-service | Orchestrates transfer logic; manages transaction state |
| account-service | Executes debit/credit operations with account locking |

### Preconditions
- Customer is authenticated (valid JWT, not expired).
- Customer has an ACTIVE account with sufficient balance.
- Destination account number exists and is ACTIVE.
- Source and destination account numbers are different.

### Main Flow

```
START
│
├─[1] Customer initiates transfer
│     Input: { toAccountNumber, amount }
│     Header: Idempotency-Key (optional, recommended)
│     Channel: POST /transactions/transfer
│     JWT: Authorization: Bearer {accessToken}
│
├─[2] API Gateway validates JWT signature
│     • Fetch public key from auth-service JWKS endpoint
│     • Verify RS256 signature; check expiry (exp claim)
│     • Extract userId from sub claim
│
├─[3] transaction-service checks idempotency
│     If Idempotency-Key header present:
│     • Query transactions WHERE idempotency_key = ?
│     • If EXISTS → Return existing transaction (HTTP 200, no re-processing)
│     • If NOT EXISTS → Proceed
│
├─[4] transaction-service resolves source account
│     • GET /internal/accounts/user/{userId} → account-service
│     • Extract fromAccountNumber from response
│
├─[5] transaction-service validates transfer conditions
│     • fromAccount != toAccount (no self-transfer)
│     • fromAccount.status == ACTIVE
│     • toAccount.status == ACTIVE (GET /internal/accounts/{toAccountNumber})
│     • fromAccount.balance >= amount
│     • amount > 0 (rounded to 2 decimal places)
│
├─[6] transaction-service creates PENDING transaction
│     • id: UUID
│     • idempotencyKey: from header (if provided)
│     • fromAccount, toAccount, amount, currency
│     • status: PENDING
│     • Persist to transaction_db.transactions
│
├─[7] transaction-service executes debit on source account
│     • POST /internal/accounts/{fromAccount}/debit
│     • account-service: SELECT FOR UPDATE (pessimistic lock)
│     • Balance check (double validation at DB layer)
│     • balance = balance - amount; save
│
├─[8] transaction-service executes credit on destination account
│     • POST /internal/accounts/{toAccount}/credit
│     • account-service: SELECT FOR UPDATE (pessimistic lock)
│     • balance = balance + amount; save
│
├─[9] transaction-service updates transaction status = SUCCESS
│     • Persist final status
│
├─[10] transaction-service returns response
│      HTTP 200
│      Body: { id, fromAccount, toAccount, amount, currency, 
│              status: SUCCESS, createdAt, updatedAt }
│
└─[11] Customer receives transfer confirmation
```

### Alternative Flows

**AF-001 — Insufficient Balance**
- Step 5: fromAccount.balance < amount.
- Log: `TRANSFER_INSUFFICIENT_BALANCE - fromAccount, balance, requested`
- HTTP 400: "Insufficient balance. Available: {balance}"
- No transaction record created. No balance modified.

**AF-002 — Source or Destination Account Not ACTIVE**
- Step 5: account status ≠ ACTIVE.
- HTTP 400: "Source/Destination account is not ACTIVE. Current status: {status}"
- No transaction record created.

**AF-003 — Destination Account Not Found**
- Step 5: GET /internal/accounts/{toAccount} returns 404.
- HTTP 400: "Account not found: {accountNumber}"

**AF-004 — Self-Transfer Attempt**
- Step 5: fromAccount == toAccount.
- HTTP 400: "Cannot transfer to the same account."

**AF-005 — Debit Succeeds but Credit Fails**
- Step 7 succeeds (balance debited).
- Step 8 fails (destination account-service call fails / DB error).
- transaction-service catches exception.
- Transaction record updated: status = FAILED, failureReason = error message.
- HTTP 500: "Transfer failed: {reason}"
- **Operations alert:** Manual reconciliation required. Balance integrity at risk.
- > *Note: Full compensating transaction / saga pattern is planned for Phase 2.*

**AF-006 — account-service Unavailable**
- Steps 4, 7, or 8: ResourceAccessException.
- HTTP 503: "Account service is currently unavailable. Please try again later."
- If idempotency key was provided, client can safely retry.

**AF-007 — Duplicate Idempotency Key (Retry Scenario)**
- Step 3: Existing transaction found with same key.
- Original transaction response returned (HTTP 200).
- No balance modification. No duplicate transaction.

### Post Conditions
- Transaction record exists with status SUCCESS or FAILED.
- Source account balance reduced by transfer amount (on success).
- Destination account balance increased by transfer amount (on success).
- Log entries exist for all steps with correlationId = idempotencyKey.
- Customer receives confirmation.

---

## PF-007 View Transaction History

### Overview
The Transaction History process enables customers and authorised bank staff to retrieve a paginated list of all financial transactions associated with an account.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Customer | Requests own transaction history |
| Teller / Operations Officer | May request history for a specific account (admin use) |
| API Gateway | Routes request; validates JWT |
| transaction-service | Queries and returns paginated history |

### Preconditions
- Requester is authenticated (valid JWT).
- Target account number is known.
- transaction-service and transaction_db are operational.

### Main Flow

```
START
│
├─[1] Customer navigates to Transaction History screen
│
├─[2] Client sends history request
│     Channel: GET /transactions/history?accountNumber={acct}&page={n}&size={s}
│     Header: Authorization: Bearer {accessToken}
│     Default: page=0, size=10
│
├─[3] API Gateway validates JWT
│
├─[4] transaction-service validates request parameters
│     • accountNumber: required, 10-digit format
│     • page: ≥ 0
│     • size: 1–100 (enforced; defaults to 10)
│
├─[5] transaction-service queries transaction_db
│     SELECT * FROM transactions 
│     WHERE from_account = ? OR to_account = ?
│     ORDER BY created_at DESC
│     LIMIT ? OFFSET ?
│     (Uses composite indexes on from_account, to_account)
│
├─[6] transaction-service calculates pagination metadata
│     • totalElements: COUNT of matching records
│     • totalPages: CEIL(totalElements / size)
│     • currentPage: page parameter
│
├─[7] transaction-service returns PagedResponse
│     HTTP 200
│     Body: {
│       content: [ {id, fromAccount, toAccount, amount, 
│                   currency, status, failureReason, 
│                   createdAt, updatedAt}, ... ],
│       page: 0,
│       size: 10,
│       totalElements: 150,
│       totalPages: 15
│     }
│
└─[8] Customer views transaction list
        Most recent transactions displayed first
        Both debit (from) and credit (to) transactions visible
```

### Alternative Flows

**AF-001 — No Transactions Found**
- Step 5 returns empty result set.
- HTTP 200 with empty content array and totalElements = 0.
- UI displays: "No transactions found."

**AF-002 — Invalid Account Number Format**
- Step 4 validation fails.
- HTTP 400: "Invalid account number format."

**AF-003 — Page Out of Range**
- Step 5: Requested page exceeds totalPages.
- HTTP 200 with empty content array (Spring Data JPA default behaviour).
- Pagination metadata reflects actual total pages.

### Post Conditions
- Paginated transaction list returned to client.
- Response includes all transactions where account is sender or receiver.
- No transaction data is modified by this operation (read-only).

---

## PF-008 Account Freeze

### Overview
The Account Freeze process allows authorised Operations Officers and Compliance Officers to suspend a customer's account, preventing all debit transactions. This is used for regulatory compliance, fraud response, or customer request.

### Actors
| Actor | Role in Process |
|-------|----------------|
| Operations Officer | Initiates account freeze based on operational trigger |
| Compliance Officer | Initiates freeze based on AML/regulatory trigger |
| account-service | Updates account status |
| Notification Service | Alerts customer via email |
| Audit System | Records the freeze action |

### Preconditions
- Account exists and is currently in ACTIVE status.
- The initiating officer has ROLE_OPERATIONS or ROLE_COMPLIANCE.
- A valid business reason is documented for the freeze action.
- Officer is authenticated with a valid session.

### Main Flow

```
START
│
├─[1] Officer identifies account requiring freeze
│     Source: AML alert / customer complaint / fraud detection
│
├─[2] Officer navigates to Account Management screen
│     (Admin portal — not accessible by customers)
│
├─[3] Officer searches for customer account
│     By: customer name, CIF number, or account number
│
├─[4] Officer reviews account summary
│     Displays: customer name, KYC status, account status, 
│               balance, recent transaction history
│
├─[5] Officer selects "Freeze Account" action
│
├─[6] System requires freeze justification
│     Input: { reason: enum[AML_INVESTIGATION, FRAUD, COURT_ORDER, 
│                             CUSTOMER_REQUEST, DORMANT], 
│              notes: free text,
│              referenceNumber: case reference (mandatory for AML/COURT) }
│
├─[7] Officer submits freeze request
│     Channel: PATCH /accounts/{accountNumber}/status
│     Body: { "status": "SUSPENDED", "reason": "...", "notes": "..." }
│
├─[8] account-service validates request
│     • Current status must be ACTIVE (cannot freeze already-frozen)
│     • Officer role: ROLE_OPERATIONS or ROLE_COMPLIANCE
│     • Reason code: must be from approved enum list
│
├─[9] account-service updates account status
│     • SET status = SUSPENDED
│     • SET updatedAt = NOW
│     • Persist to account_db.accounts
│
├─[10] Audit log entry created
│      { actor: officerId, action: ACCOUNT_FREEZE, 
│        accountNumber, reason, notes, referenceNumber, 
│        timestamp: UTC }
│
├─[11] Notification Service sends email to customer
│      Subject: "Important: Your account has been temporarily restricted."
│      Body: guidance on contact number and reference
│      (Note: email content must not disclose AML investigation per regulation)
│
└─[12] Freeze complete
        Account status = SUSPENDED
        All debit operations return HTTP 400 ("Account is not ACTIVE")
        Credits may still be processed per bank policy
```

### Alternative Flows

**AF-001 — Account Already Suspended**
- Step 8: Current status = SUSPENDED.
- HTTP 409: "Account is already suspended."
- Officer may view existing freeze details and extend review period.

**AF-002 — Account Already Closed**
- Step 8: Current status = CLOSED.
- HTTP 400: "Cannot freeze a closed account."

**AF-003 — Compliance Escalation Required**
- Step 5: Freeze reason = COURT_ORDER or AML_INVESTIGATION.
- System requires dual authorisation: Operations Officer + Compliance Officer sign-off.
- Freeze is placed in PENDING_APPROVAL status.
- Compliance Officer receives review notification.
- Compliance Officer approves → account frozen (Step 9).

**AF-004 — Account Unfreeze**
- Operations Officer initiates PATCH /accounts/{accountNumber}/status.
- Body: { "status": "ACTIVE", "reason": "INVESTIGATION_RESOLVED", "notes": "..." }
- account-service validates SUSPENDED → ACTIVE transition.
- Audit log updated. Customer notified.

### Post Conditions
- Account status = SUSPENDED in account_db.
- All debit operations on the account return an appropriate error.
- Audit record exists with officer ID, timestamp, reason, and reference number.
- Customer notified via email (using non-disclosure language for AML cases).
- Account is reactivated only by authorised officer via unfreeze process.

---

*Document End — BCP-BPF-001 v1.0*
