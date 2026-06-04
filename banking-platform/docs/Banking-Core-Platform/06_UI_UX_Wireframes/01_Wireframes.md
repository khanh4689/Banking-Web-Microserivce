# Banking Core Platform
## Document 06 — UI/UX Wireframes (Low-Fidelity)

**Document ID:** BCP-UX-001  
**Version:** 1.0  
**Status:** Draft — For Review  
**Prepared By:** Business Analysis Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

> **Note:** These wireframes are low-fidelity ASCII representations of the intended screen layouts. They communicate structural intent and interaction logic for developer implementation and stakeholder review. High-fidelity designs are produced separately by the UI/UX design team in Figma.

---

## Table of Contents

1. [WF-001 Login Screen](#wf-001-login-screen)
2. [WF-002 Registration Screen](#wf-002-registration-screen)
3. [WF-003 Email Verification Screen](#wf-003-email-verification-screen)
4. [WF-004 Dashboard / Home Screen](#wf-004-dashboard--home-screen)
5. [WF-005 Fund Transfer Screen](#wf-005-fund-transfer-screen)
6. [WF-006 Transfer Confirmation Screen](#wf-006-transfer-confirmation-screen)
7. [WF-007 Beneficiary Management Screen](#wf-007-beneficiary-management-screen)
8. [WF-008 Transaction History Screen](#wf-008-transaction-history-screen)
9. [WF-009 Customer Profile Screen](#wf-009-customer-profile-screen)
10. [WF-010 KYC Verification Screen](#wf-010-kyc-verification-screen)

---

## WF-001 Login Screen

### Screen ID: WF-001
### Screen Title: Login
### Route: /login

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                  🏦 BANKING CORE PLATFORM               │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │                  Welcome Back                     │  │
│  │            Sign in to your account               │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ 👤  Username                                │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ 🔒  Password                    [Show/Hide] │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  [ ] Remember me         Forgot Password? ──────► │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │            SIGN IN  →                       │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ─────────────── OR ─────────────────────────     │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  [G]  Sign in with Google                   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  Don't have an account?  [Create Account]         │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### UI Behaviour & Validations

| Element | Behaviour | Validation Rule |
|---------|-----------|-----------------|
| Username field | Text input, auto-focus on load | Required; min 3, max 50 chars; alphanumeric only |
| Password field | Masked input with show/hide toggle | Required; min 8 chars |
| Sign In button | Submits POST /auth/login; shows loading spinner | Disabled until both fields non-empty |
| Google Sign In | Redirects to Google OAuth2 authorisation URL | N/A |
| Forgot Password link | Navigates to /forgot-password | N/A |
| Create Account link | Navigates to /register | N/A |
| Error state | Inline error message below form: "Invalid username or password." | HTTP 401 response |
| Email not verified | Inline error: "Please verify your email before logging in. [Resend email]" | HTTP 401 with specific code |
| Session management | JWT stored in memory; refresh token in httpOnly cookie | On 401, attempt silent refresh before redirecting to login |

---

## WF-002 Registration Screen

### Screen ID: WF-002
### Screen Title: Create Account
### Route: /register

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                  🏦 BANKING CORE PLATFORM               │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │              Create Your Account                  │  │
│  │      Start your digital banking journey           │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ 👤  Username *                              │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  │     ℹ️  3–50 characters, letters and numbers │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ ✉️  Email Address *                          │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ 🔒  Password *                  [Show/Hide] │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  │     ● ● ● ● ● ○ ○ ○  Strength: Medium       │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ 🔒  Confirm Password *          [Show/Hide] │  │  │
│  │  │     ─────────────────────────────────────   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  [ ] I agree to the Terms & Conditions            │  │
│  │      and Privacy Policy                           │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │          CREATE ACCOUNT  →                  │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  Already have an account?  [Sign In]              │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Success State

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                    ✅                             │  │
│  │        Registration Successful!                   │  │
│  │                                                   │  │
│  │  We've sent a verification email to:              │  │
│  │       📧  john@example.com                       │  │
│  │                                                   │  │
│  │  Please check your inbox and click the            │  │
│  │  verification link to activate your account.      │  │
│  │                                                   │  │
│  │  The link expires in 24 hours.                    │  │
│  │                                                   │  │
│  │  Didn't receive the email?                        │  │
│  │  [Resend Verification Email]                      │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Validations

| Field | Rule | Error Message |
|-------|------|---------------|
| Username | Required; 3–50 chars; alphanumeric only; unique | "Username already taken." |
| Email | Required; valid email format; unique | "Email already registered." |
| Password | Min 8 chars; must include uppercase, lowercase, digit | "Password must be at least 8 characters and include uppercase, lowercase, and a number." |
| Confirm Password | Must match Password field | "Passwords do not match." |
| Terms checkbox | Must be checked | "You must accept the Terms and Conditions." |

---

## WF-003 Email Verification Screen

### Screen ID: WF-003
### Screen Title: Email Verification
### Route: /verify-email (reached via link in email)

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                  🏦 BANKING CORE PLATFORM               │
│                                                         │
│  ── SUCCESS STATE ────────────────────────────────────  │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │                    ✅                             │  │
│  │          Email Verified Successfully!             │  │
│  │                                                   │  │
│  │  Your account is now active. You can sign in      │  │
│  │  to access your banking services.                 │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │              GO TO LOGIN  →                 │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ── ERROR STATE (Expired/Invalid Token) ───────────────  │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │                    ❌                             │  │
│  │         Verification Link Expired                 │  │
│  │                                                   │  │
│  │  This verification link has expired or is         │  │
│  │  invalid. Links are valid for 24 hours.           │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │        RESEND VERIFICATION EMAIL            │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## WF-004 Dashboard / Home Screen

### Screen ID: WF-004
### Screen Title: Dashboard
### Route: /dashboard (authenticated)

```
┌─────────────────────────────────────────────────────────┐
│  🏦 BANKING CORE PLATFORM    [🔔 2]   [👤 John Doe  ▼] │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                MY ACCOUNT                         │  │
│  │                                                   │  │
│  │  Account Number:  1234 5678 90                    │  │
│  │  Account Status:  ● ACTIVE                        │  │
│  │  Currency:        VND                             │  │
│  │                                                   │  │
│  │                                                   │  │
│  │     AVAILABLE BALANCE                             │  │
│  │                                                   │  │
│  │     ₫  12,500,000.00                              │  │
│  │          Vietnamese Dong                          │  │
│  │                                                   │  │
│  │  [  Refresh Balance  ]                            │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ⚠️  KYC Status: PENDING — Complete verification to      │
│      unlock higher transaction limits.  [Verify Now]    │
│                                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │             │ │             │ │                 │   │
│  │  💸          │ │  👥          │ │  📋              │   │
│  │  Transfer   │ │ Beneficiary │ │   History       │   │
│  │             │ │             │ │                 │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  RECENT TRANSACTIONS                  [View All]  │  │
│  │                                                   │  │
│  │  ▼ OUT  0987654321   - ₫500,000    01 Jun 2026    │  │
│  │  ▲ IN   9876543210   + ₫1,000,000  30 May 2026   │  │
│  │  ▼ OUT  1122334455   - ₫200,000    29 May 2026    │  │
│  │  ▼ OUT  5544332211   - ₫750,000    28 May 2026    │  │
│  │                                                   │  │
│  │                    [Load More]                    │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  🏠 Home   💸 Transfer   📋 History   👤 Profile        │
└─────────────────────────────────────────────────────────┘
```

### Dashboard UI Logic

| Element | Behaviour |
|---------|-----------|
| Balance | Fetched on page load via GET /accounts/me; refresh button re-fetches |
| KYC Banner | Shown only when kycStatus = PENDING; links to KYC form |
| Recent Transactions | Top 4 from GET /transactions/history?page=0&size=4 |
| Notification Bell | Shows unread notification count; opens notification drawer |
| Transfer Button | Navigates to /transfer |
| Bottom navigation | Present on mobile; hidden on desktop (sidebar used) |

---

## WF-005 Fund Transfer Screen

### Screen ID: WF-005
### Screen Title: New Transfer
### Route: /transfer

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    TRANSFER MONEY                    [?] Help  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  FROM ACCOUNT                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Account:  1234567890        Currency: VND        │  │
│  │  Available Balance:  ₫ 12,500,000.00              │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  TO ACCOUNT                                             │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Select Saved Beneficiary:                        │  │
│  │  [ Choose beneficiary...                    ▼ ]  │  │
│  │                                                   │  │
│  │  — OR enter account number manually —             │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Destination Account Number *               │  │  │
│  │  │  [ 0 9 8 7 6 5 4 3 2 1           ] [🔍]   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  Account Holder:  NGUYEN THI B (auto-populated)   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  TRANSFER DETAILS                                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │  Amount (VND) *                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  ₫  [ 500,000                           ]   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  Minimum: ₫ 1,000     Maximum: ₫ 500,000,000     │  │
│  │                                                   │  │
│  │  Transfer Note (Optional)                        │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  [ Payment for invoice #1234           ]    │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  Max 100 characters                               │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │         REVIEW TRANSFER  →                        │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Transfer Screen Validations

| Field | Validation Rule | Error Message |
|-------|----------------|---------------|
| Destination Account | Required; 10 digits; must exist on platform; must ≠ own account | "Account not found" / "Cannot transfer to own account" |
| Amount | Required; numeric; min 1,000; max 500,000,000; ≤ available balance | "Minimum transfer is ₫1,000" / "Insufficient balance. Available: ₫{x}" |
| Account lookup | On blur or [🔍] click: GET /internal/accounts/{number} | Auto-populates account holder name; shows error if not found |
| Review button | Disabled until destination resolved and amount valid | — |

---

## WF-006 Transfer Confirmation Screen

### Screen ID: WF-006
### Screen Title: Confirm Transfer
### Route: /transfer/confirm

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    CONFIRM TRANSFER                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Please review and confirm your transfer details        │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │  FROM                                             │  │
│  │  Account Number:    1234567890                    │  │
│  │  Account Holder:    John Doe                      │  │
│  │                                                   │  │
│  │  TO                                               │  │
│  │  Account Number:    0987654321                    │  │
│  │  Account Holder:    Nguyen Thi B                  │  │
│  │                                                   │  │
│  │  ─────────────────────────────────────────────   │  │
│  │                                                   │  │
│  │  TRANSFER AMOUNT     ₫  500,000.00                │  │
│  │  CURRENCY            VND                          │  │
│  │  NOTE                Payment for invoice #1234    │  │
│  │                                                   │  │
│  │  TRANSACTION FEE     ₫  0.00  (Free)              │  │
│  │  ─────────────────────────────────────────────   │  │
│  │  TOTAL DEDUCTED      ₫  500,000.00                │  │
│  │                                                   │  │
│  │  Balance After:      ₫  12,000,000.00             │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  ✅  CONFIRM & TRANSFER                           │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  [  Cancel  ]                                           │
│                                                         │
│  ── SUCCESS STATE ─────────────────────────────────    │
│  ┌───────────────────────────────────────────────────┐  │
│  │               ✅ Transfer Successful!             │  │
│  │                                                   │  │
│  │  ₫ 500,000 sent to 0987654321                    │  │
│  │  Transaction Ref: 550e8400-e29b-41d4-a716-...    │  │
│  │  Date: 04 Jun 2026, 10:32:15                      │  │
│  │                                                   │  │
│  │  [  New Transfer  ]   [  View History  ]          │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ── FAILURE STATE ─────────────────────────────────    │
│  ┌───────────────────────────────────────────────────┐  │
│  │               ❌ Transfer Failed                  │  │
│  │                                                   │  │
│  │  Reason: Destination account is not ACTIVE.       │  │
│  │                                                   │  │
│  │  [  Try Again  ]   [  Contact Support  ]          │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## WF-007 Beneficiary Management Screen

### Screen ID: WF-007
### Screen Title: Beneficiaries
### Route: /beneficiaries

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    BENEFICIARIES              [+ Add New]       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  🔍  Search beneficiaries...                      │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  SAVED BENEFICIARIES (3)                                │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  👤  Nguyen Thi B                          [···] │  │
│  │      Account: 0987654321     VCB                  │  │
│  │      Alias:   "Office Rent"                       │  │
│  │                              [Transfer] [Delete]  │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  👤  Tran Van C                            [···] │  │
│  │      Account: 1122334455     Vietcombank          │  │
│  │      Alias:   "Freelancer payment"                │  │
│  │                              [Transfer] [Delete]  │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  👤  Le Thi D                              [···] │  │
│  │      Account: 5544332211     ACB                  │  │
│  │      Alias:   "Sister"                            │  │
│  │                              [Transfer] [Delete]  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ── ADD NEW BENEFICIARY FORM (expanded) ───────────    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Account Number *                                 │  │
│  │  [ ─────────────────────── ]   [🔍 Verify]       │  │
│  │                                                   │  │
│  │  Account Holder: (auto-populated after verify)    │  │
│  │  ─────────────────────────────────────────────   │  │
│  │  Nickname / Alias                                 │  │
│  │  [ ─────────────────────── ]                     │  │
│  │                                                   │  │
│  │  Bank:  [This Bank (Platform)    ▼]              │  │
│  │                                                   │  │
│  │  [  Cancel  ]       [  Save Beneficiary  ]       │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## WF-008 Transaction History Screen

### Screen ID: WF-008
### Screen Title: Transaction History
### Route: /transactions/history

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    TRANSACTION HISTORY                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Account: 1234567890          [📅 Filter by Date ▼]    │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  🔍 Search by account or amount...               │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  SHOWING 10 of 45 TRANSACTIONS                          │
│                                                         │
│  ── June 2026 ─────────────────────────────────────    │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  ▼ Sent                          04 Jun 2026      │  │
│  │  To: 0987654321 (Nguyen Thi B)                    │  │
│  │  Ref: 550e8400-e29b...                            │  │
│  │                                    - ₫500,000    │  │
│  │                              ✅ SUCCESS           │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  ▲ Received                      02 Jun 2026      │  │
│  │  From: 9876543210 (Company ABC)                   │  │
│  │  Ref: 6ba7b810-9dad...                            │  │
│  │                                  + ₫5,000,000    │  │
│  │                              ✅ SUCCESS           │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  ▼ Sent                          01 Jun 2026      │  │
│  │  To: 1122334455                                   │  │
│  │  Ref: 6ba7b811-9dad...                            │  │
│  │                                    - ₫200,000    │  │
│  │                              ❌ FAILED            │  │
│  │                  Reason: Destination not ACTIVE   │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  ▲ Received                      30 May 2026      │  │
│  │  From: 5544332211                                 │  │
│  │                                  + ₫1,000,000    │  │
│  │                              ✅ SUCCESS           │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌─── PAGINATION ───────────────────────────────────┐   │
│  │  [← Prev]  Page 1 of 5  [Next →]                │   │
│  │   Showing 1–10 of 45 transactions                │   │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Transaction History UI Logic

| Element | Behaviour |
|---------|-----------|
| Transaction row colour | Green for credit (▲); red for debit (▼) |
| Status badge | Green ✅ for SUCCESS; red ❌ for FAILED; orange ⏳ for PENDING |
| FAILED row expansion | Click to expand and show full failureReason |
| Date filter | Calls API with `fromDate` and `toDate` query params |
| Pagination | Calls GET /transactions/history?page=N&size=10 |

---

## WF-009 Customer Profile Screen

### Screen ID: WF-009
### Screen Title: My Profile
### Route: /profile

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    MY PROFILE                    [✏️ Edit]      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │                                                   │  │
│  │             ┌──────────────┐                      │  │
│  │             │              │                      │  │
│  │             │   👤 Avatar  │  [Change Photo]      │  │
│  │             │              │                      │  │
│  │             └──────────────┘                      │  │
│  │                                                   │  │
│  │  John Doe                                         │  │
│  │  KYC Status:  ⚠️  PENDING  [Complete KYC]         │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  PERSONAL INFORMATION                                   │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Full Name          John Doe                [✏️]  │  │
│  │  Username           john_doe                      │  │
│  │  Email              john@example.com              │  │
│  │                     ✅ Verified                   │  │
│  │  Phone              +84 912 345 678         [✏️]  │  │
│  │  Member Since       01 Jan 2026                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  SECURITY                                               │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Password             ●●●●●●●●      [Change]      │  │
│  │  Two-Factor Auth      Disabled      [Enable]      │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  [  SIGN OUT  ]                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Edit Profile Mode

```
┌─────────────────────────────────────────────────────────┐
│  ← Cancel   EDIT PROFILE               [💾 Save]       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Full Name *                                            │
│  ┌───────────────────────────────────────────────────┐  │
│  │  [ John Doe                                    ]  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  Phone Number                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  [ +84 912 345 678                             ]  │  │
│  └───────────────────────────────────────────────────┘  │
│  Format: +84XXXXXXXXX  (E.164 standard)                 │
│                                                         │
│  Profile Photo                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │  [  Choose File...  ]  or drag and drop           │  │
│  │  Supported: JPG, PNG. Max size: 2MB               │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ⚠️  Email and Username cannot be changed after         │
│     registration.                                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## WF-010 KYC Verification Screen

### Screen ID: WF-010
### Screen Title: KYC Verification
### Route: /kyc-verification

```
┌─────────────────────────────────────────────────────────┐
│  ← Back    IDENTITY VERIFICATION                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ── STEP INDICATOR ────────────────────────────────    │
│  (1) Personal Info ──► (2) Documents ──► (3) Review    │
│        ✅                   ●                  ○        │
│                                                         │
│  ── STEP 1: Personal Information ──────────────────    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Full Legal Name *                                │  │
│  │  [ Nguyen Van A                               ]   │  │
│  │                                                   │  │
│  │  Date of Birth *                                  │  │
│  │  [ DD / MM / YYYY                             ]   │  │
│  │                                                   │  │
│  │  Nationality *                                    │  │
│  │  [ Vietnamese                           ▼    ]   │  │
│  │                                                   │  │
│  │  ID Type *                                        │  │
│  │  ○ National ID    ○ Passport    ○ Driver License  │  │
│  │                                                   │  │
│  │  ID Number *                                      │  │
│  │  [ 012345678901                               ]   │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ── STEP 2: Document Upload ────────────────────────    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  National ID — Front Side *                       │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  [📷 Upload or Take Photo]                  │  │  │
│  │  │  Supported: JPG, PNG, PDF. Max 5MB          │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  National ID — Back Side *                        │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  [📷 Upload or Take Photo]                  │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  Selfie with ID *                                 │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  [📷 Take Selfie]                           │  │  │
│  │  │  Hold your ID next to your face             │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │          SUBMIT FOR VERIFICATION                  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ── UNDER REVIEW STATE ─────────────────────────────   │
│  ┌───────────────────────────────────────────────────┐  │
│  │              ⏳ Under Review                      │  │
│  │                                                   │  │
│  │  Your documents have been submitted and are       │  │
│  │  being reviewed by our team.                      │  │
│  │  Estimated time: 1–2 business days                │  │
│  │                                                   │  │
│  │  We'll notify you by email once complete.         │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### KYC Screen Validations

| Field | Validation Rule | Error Message |
|-------|----------------|---------------|
| Full Legal Name | Required; 2–100 chars; no special characters except spaces and hyphens | "Please enter your full legal name as it appears on your ID." |
| Date of Birth | Required; customer must be minimum 18 years old | "You must be at least 18 years old to open an account." |
| ID Number | Required; format depends on selected ID type | "Invalid ID number format for selected document type." |
| Document files | Required; JPG/PNG/PDF; max 5MB per file | "File too large. Maximum 5MB." / "Invalid file type." |
| Selfie | Required; face must be clearly visible (manual review in Phase 1) | "Please ensure your face is clearly visible in the selfie." |

---

*Document End — BCP-UX-001 v1.0*
