# Banking Core Platform
## Document 01 — Business Context, Vision & Project Overview

**Document ID:** BCP-POV-001  
**Version:** 1.0  
**Status:** Approved  
**Prepared By:** Business Analysis Team  
**Review Date:** June 2026  
**Classification:** Internal — Confidential  

---

## Table of Contents

1. [Business Context](#1-business-context)
2. [Problem Statement](#2-problem-statement)
3. [Project Vision](#3-project-vision)
4. [Business Objectives](#4-business-objectives)
5. [Project Scope](#5-project-scope)
6. [Stakeholder Register](#6-stakeholder-register)
7. [Banking Terminology Glossary](#7-banking-terminology-glossary)
8. [Technology Architecture Summary](#8-technology-architecture-summary)

---

## 1. Business Context

### 1.1 Industry Background

The banking industry is undergoing a structural transformation driven by regulatory mandates, customer expectations for real-time digital services, and competitive pressure from fintech disruptors. Traditional core banking systems — typically monolithic COBOL or legacy Java applications — were architected for batch processing, overnight reconciliation cycles, and branch-centric service delivery. These systems are increasingly inadequate to support the demands of a 24/7 digital economy.

Financial institutions that fail to modernise their digital infrastructure face erosion of their retail customer base, reduced ability to comply with evolving Anti-Money Laundering (AML) and Know Your Customer (KYC) regulations, and inability to integrate with open banking APIs mandated under emerging regulatory frameworks.

The rise of digital-native banks and super-apps in Southeast Asian markets — particularly Vietnam, Indonesia, and Thailand — has demonstrated that customers now expect instant account opening, real-time fund transfers, and full self-service capabilities from their mobile devices. Incumbent banks must respond with equivalent capability or risk customer attrition.

### 1.2 Organisational Context

The sponsoring organisation is a mid-tier retail commercial bank operating in the Vietnamese market with approximately 1.2 million retail customers, 200+ branch network, and a growing mobile customer base. The bank's existing core banking system was deployed over a decade ago and has accumulated significant technical debt. The system operates as a monolithic application requiring scheduled maintenance windows, cannot scale horizontally to absorb traffic peaks during salary processing periods, and has no event-driven integration capability.

Key pain points identified by the business and technology leadership:

- **Customer Onboarding Friction:** New customer registration requires branch visit and paper-based KYC documentation, taking 3–5 business days. Customer drop-off during this process is estimated at 42%.
- **Transaction Processing Latency:** Domestic fund transfers complete in 2–4 hours due to batch processing cycles. Customers expect near-instantaneous transfers.
- **Operational Inefficiency:** Tellers manually process high volumes of requests that should be self-service, increasing operational cost per transaction.
- **Compliance Risk:** The existing system lacks automated transaction monitoring, creating manual AML review backlogs and regulatory exposure.
- **Scalability Constraints:** The monolithic architecture cannot scale individual services independently. During peak periods (end of month, public holidays) the entire system degrades.
- **Auditability Gaps:** End-to-end audit trails across customer actions are incomplete, creating difficulties during regulatory examinations.

---

## 2. Problem Statement

> *The bank's existing digital banking infrastructure cannot support the volume, velocity, and variety of customer interactions required in the modern digital banking landscape. The organisation requires a purpose-built, cloud-native, microservices-based digital banking platform that delivers real-time account management and fund transfer capabilities, automates KYC onboarding processes, enforces regulatory compliance at every transaction touchpoint, and provides comprehensive audit and monitoring capabilities — all while maintaining bank-grade security and five-nines availability.*

### 2.1 Business Drivers

| Driver | Description | Priority |
|--------|-------------|----------|
| Customer Experience | Reduce onboarding time from days to minutes via digital KYC | Critical |
| Operational Efficiency | Automate routine transactions to reduce teller workload by 60% | High |
| Regulatory Compliance | Implement automated AML transaction monitoring and audit trails | Critical |
| Revenue Growth | Enable 24/7 self-service banking to increase transaction volumes | High |
| Technology Modernisation | Replace monolithic legacy system with scalable microservices | High |
| Risk Reduction | Eliminate single points of failure and improve system resilience | High |

---

## 3. Project Vision

> **"Deliver a secure, real-time, fully digital banking platform that empowers customers to manage their financial lives from any device at any time, while providing the bank with the operational, compliance, and analytical capabilities to serve them safely and sustainably."**

### 3.1 Strategic Alignment

The Banking Core Platform directly supports the bank's five-year digital transformation strategy on three dimensions:

1. **Customer Centricity** — Frictionless, self-service digital experiences reduce dependency on branch channels and increase customer satisfaction scores (NPS target: +25 points within 12 months of launch).
2. **Operational Excellence** — Event-driven microservices architecture reduces time-to-market for new products from 6 months to 6 weeks; reduces per-transaction cost by automating manual workflows.
3. **Regulatory Strength** — Built-in KYC, AML, audit logging, and data retention capabilities reduce compliance risk and preparation time for regulatory examinations.

---

## 4. Business Objectives

| Ref | Objective | Measurable Target | Target Date |
|-----|-----------|-------------------|-------------|
| BO-001 | Reduce customer onboarding time | From 5 business days to under 10 minutes (digital KYC) | Q4 2026 |
| BO-002 | Achieve real-time domestic fund transfers | Transaction processing under 5 seconds end-to-end | Q4 2026 |
| BO-003 | Increase digital self-service rate | 80% of routine transactions completed without teller involvement | Q1 2027 |
| BO-004 | Ensure regulatory compliance | 100% of transactions covered by automated AML screening and audit logging | Q4 2026 |
| BO-005 | Achieve platform availability | 99.9% uptime SLA (maximum 8.7 hours unplanned downtime per year) | Ongoing |
| BO-006 | Scale transaction throughput | Platform handles minimum 5,000 concurrent users and 500 TPS peak | Q4 2026 |
| BO-007 | Reduce customer onboarding dropout rate | From 42% to under 10% with digital onboarding | Q1 2027 |
| BO-008 | Implement complete audit trail | 100% of customer-initiated actions captured in immutable audit log | Q4 2026 |

---

## 5. Project Scope

### 5.1 In Scope

The Banking Core Platform Phase 1 encompasses the following functional modules:

| Module | Scope Description |
|--------|-------------------|
| **Customer Registration** | Digital self-service registration via mobile/web channel; username, email, password with email verification workflow |
| **eKYC Verification** | Know Your Customer identity verification process; KYC status management (PENDING → VERIFIED → REJECTED) |
| **Authentication & Authorisation** | JWT-based authentication (RS256); refresh token rotation with theft detection; Google OAuth2 social login; role-based access control (ROLE_USER, ROLE_ADMIN) |
| **Customer Profile Management** | User profile creation, retrieval and update; avatar, full name, phone number, KYC status management |
| **Account Management** | Current/savings account creation (automatically provisioned on registration); account status management (ACTIVE, SUSPENDED, CLOSED); balance enquiry |
| **Fund Transfer** | Real-time domestic fund transfer between accounts; idempotency controls to prevent duplicate transfers; debit/credit operations with pessimistic locking |
| **Transaction History** | Paginated transaction history retrieval for any account; transaction status tracking (PENDING, SUCCESS, FAILED) |
| **Notification Service** | Email notifications for registration, email verification, password reset, and key account events |
| **Audit Logging** | Distributed tracing via MDC Trace ID; request/response audit logging at API Gateway; structured JSON log aggregation |
| **Service Infrastructure** | API Gateway routing; Service Discovery (Eureka); Kafka event streaming; PostgreSQL databases per service; Docker containerisation; CI/CD pipeline |

### 5.2 Out of Scope — Phase 1

The following capabilities are explicitly excluded from Phase 1 and deferred to future releases:

| Capability | Rationale for Exclusion |
|------------|-------------------------|
| Loan Origination & Management | Requires credit scoring integration with external bureau systems |
| Fixed Deposit / Term Deposit Products | Requires interest calculation engine and separate product catalogue |
| Foreign Currency Exchange | Requires integration with FX rate feed providers and SWIFT messaging |
| Cheque Processing | Branch-dependent workflow; to be addressed in branch transformation programme |
| Debit/Credit Card Issuance & Management | Requires card scheme integration (Visa/Mastercard) and HSM infrastructure |
| ATM Network Integration | Physical infrastructure dependent; separate workstream |
| Corporate/Business Banking | Commercial credit, trade finance, and cash management are a separate product programme |
| Open Banking APIs (PSD2/Open API) | Deferred to Phase 2 pending regulatory framework finalisation |
| Core Banking System Integration (real-time) | Phased integration; Phase 1 is greenfield; Phase 2 integrates with legacy CBS |
| Advanced Analytics / BI Reporting | Data warehouse and reporting layer deferred to analytics programme |
| Beneficiary Management (External Banks) | Inter-bank beneficiary directory integration deferred; Phase 1 covers own-bank transfers |

### 5.3 Assumptions

1. The platform will be deployed on a private cloud infrastructure managed by the bank's IT operations team.
2. A PostgreSQL 16 database cluster with appropriate backup and replication is provisioned by infrastructure.
3. An Apache Kafka cluster with sufficient partition and replication configuration is provisioned by infrastructure.
4. SMTP relay service (Gmail SMTP or equivalent enterprise relay) is available for notification emails.
5. The bank's existing identity management system will not be integrated in Phase 1; auth-service is a standalone identity provider.
6. The Vietnamese Dong (VND) is the primary operating currency for Phase 1.
7. External KYC vendor API (for document verification) integration is addressed in a separate eKYC sub-project.

### 5.4 Constraints

1. The platform must comply with the State Bank of Vietnam (SBV) Circular 19/2016/TT-NHNN on e-banking security.
2. Customer PII must be stored within Vietnam's territorial borders per Cybersecurity Law 2018.
3. All inter-service communication must be encrypted in transit.
4. RSA key material must be rotated on a defined schedule (minimum annually).
5. The development team operates in a 2-week Agile sprint cadence.

---

## 6. Stakeholder Register

### 6.1 Primary Stakeholders

| Stakeholder | Role | Interest | Influence | Key Concerns |
|-------------|------|----------|-----------|--------------|
| **Retail Banking Head** | Executive Sponsor | Customer acquisition, revenue growth | High | Time-to-market, customer NPS |
| **Chief Information Officer (CIO)** | Technology Sponsor | Platform modernisation, technical debt | High | Architecture soundness, vendor risk |
| **Chief Risk Officer (CRO)** | Risk Governance | Regulatory compliance, operational risk | High | AML controls, audit trail completeness |
| **Product Owner** | Delivery Owner | Feature completeness, backlog priority | High | Sprint velocity, acceptance criteria |
| **IT Development Lead** | Technical Lead | Architecture, code quality, delivery | High | Technical feasibility, non-functional requirements |

### 6.2 User Stakeholders

| Actor | Description | System Role | Functional Areas |
|-------|-------------|-------------|-----------------|
| **Customer** | Retail bank customer accessing the platform via mobile/web | ROLE_USER | Registration, Login, Profile, Account, Transfer, History |
| **Teller** | Branch staff assisting customers with transactions | ROLE_TELLER | Account enquiry, transaction initiation on behalf of customer |
| **Bank Administrator** | System administrator managing platform configuration and user roles | ROLE_ADMIN | User management, account status management, system monitoring |
| **Operations Officer** | Back-office staff handling operational exceptions and reconciliation | ROLE_OPERATIONS | Transaction review, account freeze/unfreeze, exception management |
| **Compliance Officer** | Regulatory compliance team member performing AML monitoring and audit reviews | ROLE_COMPLIANCE | Audit log review, transaction monitoring, regulatory reporting |

### 6.3 Supporting Stakeholders

| Stakeholder | Role | Interaction |
|-------------|------|-------------|
| **IT Infrastructure Team** | Manages cloud/on-premise infrastructure | Provides PostgreSQL, Kafka, network environments |
| **Information Security Team** | Security governance and penetration testing | Reviews JWT implementation, key management, access controls |
| **Legal & Compliance Team** | Regulatory interpretation | Advises on SBV requirements, KYC obligations, data retention |
| **Customer Service Team** | First-line customer support | Uses admin tools to resolve customer issues |
| **External KYC Vendor** | Document verification service provider | Provides eKYC API (Phase 2 integration) |
| **SMTP Provider** | Email relay service | Delivers transactional emails (verification, password reset) |

---

## 7. Banking Terminology Glossary

| Term | Definition |
|------|-----------|
| **Account Number** | A unique 10-digit numeric identifier assigned to a customer's bank account. In this platform, auto-generated as a zero-padded random 10-digit number. |
| **AML (Anti-Money Laundering)** | A set of laws, regulations, and procedures designed to prevent the generation of income through illegal activities. The platform must flag, record, and where necessary report suspicious transactions to the State Bank of Vietnam. |
| **Available Balance** | The portion of the ledger balance that is immediately accessible by the customer for withdrawal or transfer. May differ from ledger balance due to pending holds or authorised but unsettled transactions. |
| **Beneficiary** | A third party designated by the account holder to receive fund transfers. A beneficiary record typically stores the recipient's name, account number, and bank details. |
| **BCrypt** | A cryptographic password hashing function. All customer passwords are stored as BCrypt hashes; plaintext passwords are never persisted. |
| **CASA (Current Account and Savings Account)** | The primary retail banking account products. Current accounts support unlimited transactions; savings accounts typically earn interest. Phase 1 implements a unified current account model. |
| **CBS (Core Banking System)** | The legacy enterprise system that manages the bank's master ledger, including all accounts, balances, and transactions. Phase 1 is standalone; Phase 2 integrates with CBS via API. |
| **CIF (Customer Information File)** | The master record of a bank customer containing all personal information, account relationships, and KYC status. In this platform, the User Profile in user-service serves as the digital CIF. |
| **Credit** | An accounting entry that increases the balance of a liability or equity account (i.e., increases a customer's account balance). When money is transferred to an account, a credit operation is executed. |
| **Debit** | An accounting entry that decreases the balance of a liability account (i.e., decreases a customer's account balance). When money is transferred out of an account, a debit operation is executed. |
| **eKYC (Electronic Know Your Customer)** | A digital identity verification process that authenticates customer identity using electronic documents and biometric checks without requiring in-person branch visits. |
| **Idempotency Key** | A unique identifier included in an API request by the client. If the same idempotency key is submitted more than once (e.g., due to network retry), the server returns the original result without re-processing the operation. Critical for preventing duplicate transfers. |
| **JWT (JSON Web Token)** | An open standard (RFC 7519) for securely transmitting information between parties as a compact, self-contained JSON object. This platform uses RS256 (RSA Signature with SHA-256) signed JWTs for authentication. |
| **JWKS (JSON Web Key Set)** | A JSON document that contains a set of public keys used to verify JWT signatures. The auth-service exposes a JWKS endpoint at `/.well-known/jwks.json`. |
| **KYC (Know Your Customer)** | A regulatory requirement for banks to verify the identity of their clients. KYC status values in this platform: PENDING → IN_REVIEW → VERIFIED / REJECTED. |
| **Ledger Balance** | The balance of an account as recorded in the bank's accounting ledger, including all posted transactions. Contrast with available balance. |
| **MDC (Mapped Diagnostic Context)** | A Logback/SLF4J feature that associates contextual information (such as a Trace ID) with each log entry. Used in this platform to correlate all log entries from a single request across services. |
| **Optimistic Locking** | A concurrency control strategy where a version number on a database record is incremented on each update. If two processes attempt to update simultaneously, one will receive a version conflict error. Used in Account entity via `@Version`. |
| **Pessimistic Locking** | A concurrency control strategy where a database row is locked exclusively during a transaction to prevent concurrent modifications. Used during debit and credit operations on accounts to prevent balance corruption. |
| **PII (Personally Identifiable Information)** | Any information that can be used to identify a specific individual, such as name, email address, phone number, or national ID. Subject to data protection regulations. |
| **Refresh Token** | A long-lived credential (7 days) used to obtain a new access token without requiring re-authentication. Stored as a SHA-256 hash in the database; the raw token is returned only at issuance time. |
| **RSA (Rivest–Shamir–Adleman)** | An asymmetric cryptographic algorithm. The platform uses 2048-bit RSA key pairs: the private key signs JWTs in auth-service; the public key verifies them in all resource services. |
| **SBV (State Bank of Vietnam)** | The central bank and primary financial regulatory authority in Vietnam. Issues regulations governing e-banking, KYC, AML, and data protection for licensed banks. |
| **Trace ID / Correlation ID** | A unique identifier (UUID) injected into every inbound request and propagated across all service calls. Enables end-to-end distributed tracing across microservices. |
| **Transaction** | A financial operation that transfers value between accounts. Each transaction has a status lifecycle: PENDING → SUCCESS / FAILED. |
| **VND (Vietnamese Dong)** | The official currency of Vietnam (ISO 4217 code: VND). The primary currency for Phase 1. All monetary values use NUMERIC(19,2) precision. |

---

## 8. Technology Architecture Summary

The Banking Core Platform is implemented as a collection of independently deployable Spring Boot microservices, each owning its own database and communicating through either synchronous REST APIs or asynchronous Kafka events.

### 8.1 Service Topology

```
                    ┌─────────────────────────┐
                    │       API Gateway        │  Port: 8080
                    │  (Spring Cloud Gateway)  │  Routes by path prefix
                    └───────────┬─────────────┘  via Eureka load balancing
                                │
          ┌─────────────────────┼──────────────────────┐
          │                     │                      │
   ┌──────▼──────┐      ┌───────▼──────┐      ┌───────▼──────┐
   │auth-service │      │ user-service │      │account-service│
   │  Port 8082  │      │  Port 8081   │      │  Port 8083    │
   │  auth_db    │      │  user_db     │      │  account_db   │
   └──────┬──────┘      └──────┬───────┘      └───────┬───────┘
          │                    │                      │  Internal REST
          │  Kafka user-events │                      │
          │  (AFTER_COMMIT)    │              ┌───────▼──────────┐
          └────────────────────►──────────────►  transaction-svc  │
                                              │  Port 8084        │
                                              │  transaction_db   │
                                              └───────────────────┘

   ┌───────────────────────────────────────────────────────────┐
   │                   Apache Kafka                             │
   │   Topic: user-events  (producer: auth-service)            │
   │   Consumer Groups: user-service-group, account-svc-group  │
   └───────────────────────────────────────────────────────────┘

   ┌───────────────────────────────────────────────────────────┐
   │               PostgreSQL 16                                │
   │   auth_db | user_db | account_db | transaction_db         │
   └───────────────────────────────────────────────────────────┘

   ┌───────────────────────────────────────────────────────────┐
   │         Eureka Discovery Service  Port: 8761              │
   └───────────────────────────────────────────────────────────┘
```

### 8.2 Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Language | Java | 17 | Core development language |
| Framework | Spring Boot | 3.2.5 | Microservice application framework |
| API Gateway | Spring Cloud Gateway | 2023.0.1 | Routing, load balancing, cross-cutting concerns |
| Service Discovery | Netflix Eureka | 2023.0.1 | Service registration and discovery |
| Security | Spring Security + jjwt | 0.12.5 | JWT RS256 token generation and validation |
| OAuth2 | Spring OAuth2 Client | 3.2.5 | Google social login |
| Messaging | Apache Kafka | 7.6.0 (Confluent) | Asynchronous event streaming |
| Database | PostgreSQL | 16 | Relational data store (per-service schema) |
| ORM | Spring Data JPA / Hibernate | — | Object-relational mapping |
| Migrations | Flyway | — | Database schema versioning |
| Build | Gradle | Multi-module | Build automation |
| Containerisation | Docker + Docker Compose | — | Service packaging and orchestration |
| CI/CD | GitHub Actions | — | Automated build and test pipeline |
| Logging | Logback + MDC | — | Structured logging with distributed trace correlation |
| Code Generation | Lombok | — | Boilerplate reduction |

---

*Document End — BCP-POV-001 v1.0*
