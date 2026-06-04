# Banking Core Platform — BA Project Documentation Package

**Version:** 1.0  
**Date:** June 2026  
**Prepared By:** Business Analysis Team  
**Classification:** Internal — Confidential  

---

## Package Overview

This documentation package constitutes the complete Business Analysis deliverable for the **Banking Core Platform** — a production-grade digital banking platform built on a Spring Boot microservices architecture.

The package is structured across 9 folders, each containing a professional-grade deliverable suitable for use in a BA/SA portfolio and as working project documentation.

---

## Document Index

| # | Folder | Document | Doc ID | Description |
|---|--------|----------|--------|-------------|
| 1 | `01_Project_Overview` | Business Context & Vision | BCP-POV-001 | Business problem, project vision, objectives, scope, stakeholders, glossary |
| 2 | `02_Business_Requirements` | Business Requirements | BCP-BR-001 | 22 business requirements, NFRs, regulatory requirements (KYC/AML/SBV) |
| 3 | `03_BPMN_Process_Flows` | Process Flows | BCP-BPF-001 | 8 end-to-end BPMN-style process flows with actors, preconditions, alternative flows |
| 4 | `04_Data_Model` | Data Model | BCP-DM-001 | Conceptual model, logical model, ERD, complete data dictionary (all tables) |
| 5 | `05_Product_Backlog_and_Stories` | Product Backlog | BCP-PB-001 | 8 Epics, 38 User Stories with Gherkin acceptance criteria |
| 6 | `06_UI_UX_Wireframes` | UI/UX Wireframes | BCP-UX-001 | 10 ASCII low-fidelity wireframes with UI behaviour and validation specs |
| 7 | `07_Functional_Solution_Design` | Functional Solution Design | BCP-FSD-001 | Per-module FSD for all 7 modules with APIs, rules, error codes, audit specs |
| 8 | `08_Data_Mapping_Specifications` | Data Mapping | BCP-DMS-001 | 10 mapping tables: client → gateway → service → DB with transformation logic |
| 9 | `09_Testing_and_UAT` | Test Cases & UAT | BCP-TST-001 | 30 test cases (positive/negative/boundary) + 20 UAT scenarios + sign-off template |

---

## Platform Summary

| Attribute | Value |
|-----------|-------|
| **Platform Name** | Banking Core Platform |
| **Architecture** | Spring Boot Microservices |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | JWT RS256 + Spring Security OAuth2 |
| **Messaging** | Apache Kafka (user-events topic) |
| **Database** | PostgreSQL 16 (per-service schemas) |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway |
| **Containerisation** | Docker + Docker Compose |
| **CI/CD** | GitHub Actions |
| **Primary Currency** | Vietnamese Dong (VND) |

## Services

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| api-gateway | 8080 | — | External traffic routing |
| auth-service | 8082 | auth_db | Authentication, JWT, OAuth2, email verification |
| user-service | 8081 | user_db | Customer profile / CIF management |
| account-service | 8083 | account_db | Account lifecycle, balance operations |
| transaction-service | 8084 | transaction_db | Fund transfers, transaction history |
| discovery-service | 8761 | — | Eureka service registry |

---

*Banking Core Platform BA Documentation Package v1.0*
