# Banking Platform — Microservices

A production-ready banking backend built with Spring Boot microservices.

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  (Spring Cloud) │
                        └────────┬────────┘
                                 │  routes
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
   ┌──────▼──────┐       ┌───────▼──────┐      ┌───────▼──────┐
   │ auth-service│       │ user-service │      │account-service│
   │   :8082     │       │   :8081      │      │   :8083       │
   └──────┬──────┘       └──────┬───────┘      └───────┬───────┘
          │                     │                      │
          │              ┌──────▼───────┐              │
          │              │  transaction │◄─────────────┘
          │              │   service    │  REST (internal)
          │              │   :8084      │
          │              └──────────────┘
          │
   ┌──────▼──────────────────────────────────┐
   │              Kafka                       │  user-events topic
   │         (user registration events)       │
   └──────────────────────────────────────────┘

   ┌──────────────────────────────────────────┐
   │           PostgreSQL :5432               │
   │  auth_db | user_db | account_db | tx_db  │
   └──────────────────────────────────────────┘

   ┌──────────────────────────────────────────┐
   │        Eureka Discovery :8761            │
   └──────────────────────────────────────────┘
```

## Tech Stack

| Layer         | Technology                              |
|---------------|-----------------------------------------|
| Language      | Java 17                                 |
| Framework     | Spring Boot 3.2.5                       |
| Security      | Spring Security + JWT (RSA)             |
| Service Mesh  | Spring Cloud Eureka + API Gateway       |
| Messaging     | Apache Kafka                            |
| Database      | PostgreSQL 16                           |
| Migrations    | Flyway                                  |
| Build         | Gradle (multi-module)                   |
| Containers    | Docker + Docker Compose                 |
| CI/CD         | GitHub Actions                          |
| Logging       | Logback + MDC TraceId                   |

## Services

| Service             | Port | Description                                      |
|---------------------|------|--------------------------------------------------|
| api-gateway         | 8080 | Routes all external traffic                      |
| auth-service        | 8082 | Register, login, JWT, email verification         |
| user-service        | 8081 | User profile management                          |
| account-service     | 8083 | Bank account creation, balance operations        |
| transaction-service | 8084 | Money transfers with idempotency                 |
| discovery-service   | 8761 | Eureka service registry                          |

## How to Run

### Prerequisites
- Docker Desktop
- Java 17 (for local builds)

### 1. Build all JARs

```bash
cd banking-platform
./gradlew bootJar --no-daemon
```

### 2. Start with Docker Compose

```bash
docker compose up -d
```

### 3. Verify all services are healthy

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### 4. Test the API

```bash
# Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```

## API Endpoints

### Auth (`/auth`)
| Method | Path                        | Description              |
|--------|-----------------------------|--------------------------|
| POST   | /auth/register              | Register new user        |
| POST   | /auth/login                 | Login, returns JWT       |
| GET    | /auth/verify-email?token=   | Verify email             |
| POST   | /auth/refresh               | Refresh access token     |
| POST   | /auth/forgot-password       | Send reset email         |
| POST   | /auth/reset-password        | Reset password           |

### Users (`/users`)
| Method | Path        | Description              |
|--------|-------------|--------------------------|
| GET    | /users/me   | Get current user profile |
| PUT    | /users/me   | Update profile           |
| GET    | /users/{id} | Get profile by ID        |

### Accounts (`/accounts`)
| Method | Path          | Description              |
|--------|---------------|--------------------------|
| POST   | /accounts     | Create account           |
| GET    | /accounts/me  | Get my account           |

### Transactions (`/transactions`)
| Method | Path                    | Description              |
|--------|-------------------------|--------------------------|
| POST   | /transactions/transfer  | Transfer money           |
| GET    | /transactions/history   | Transaction history      |

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`):
- Triggers on push/PR to `main` and `develop`
- Matrix build for all 4 services in parallel
- Runs unit tests — fails pipeline if any test fails
- Builds Docker image per service

## Logging

Every request gets a `traceId` injected via `OncePerRequestFilter` + MDC.

Log format:
```
2026-04-07 10:00:00.000 [INFO ] [auth-service] [traceId=abc123def456] [http-nio-1] c.b.a.s.AuthService - Register attempt for username=john
```

Logs are written to `logs/<service-name>.log` inside each container (mounted via Docker volumes).
