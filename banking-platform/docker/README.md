# Docker Dev Environment — Auth → Kafka → User Pipeline

## Architecture

```
POST /api/auth/register
        │
        ▼
  auth-service (8082)  →  PostgreSQL: auth_db
        │  publishes UserCreatedEvent
        ▼
  Kafka topic: user-events
        │  consumed by
        ▼
  user-service (8081)  →  PostgreSQL: user_db → users_profile table
```

---

## Step-by-Step Testing

### Step 1 — Build & Start Docker

```bash
cd banking-platform

# Full reset (required on first run or after DB name changes)
docker compose down -v
docker system prune -f

# Build and start all services
docker compose up --build -d
```

Watch startup order: postgres → zookeeper → kafka → discovery-service → auth-service → user-service

Check all containers are running:
```bash
docker compose ps
```

Wait until all services show `healthy` (about 3–5 minutes on first build).

---

### Step 2 — Verify Databases Exist

```bash
docker exec -it postgres psql -U postgres -c "\l"
```

Must show:
- `auth_db`
- `user_db`

If missing, the init script didn't run. This happens when the volume already existed with old data.
Fix: `docker compose down -v` then `docker compose up --build -d`

---

### Step 3 — Verify Flyway Ran

Check auth-service created its tables:
```bash
docker exec -it postgres psql -U postgres -d auth_db -c "\dt"
```
Expected tables: `users`, `refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`

Check user-service created its table:
```bash
docker exec -it postgres psql -U postgres -d user_db -c "\dt"
```
Expected table: `users_profile`

---

### Step 4 — Create Kafka Topic (auto-created, but verify)

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server kafka:29092 \
  --list
```

Should show `user-events` after first registration call.

---

### Step 5 — Call Auth Service Register API

```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "Password123!"
  }'
```

Expected: `200 OK`

---

### Step 6 — Verify Kafka Message

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:29092 \
  --topic user-events \
  --from-beginning \
  --max-messages 5
```

Expected JSON:
```json
{"id":"<uuid>","email":"testuser@example.com","phone":null,"username":"testuser"}
```

---

### Step 7 — Verify PostgreSQL Insert

```bash
docker exec -it postgres psql -U postgres -d user_db
```

```sql
SELECT id, auth_id, email, username, kyc_status, created_at
FROM users_profile
ORDER BY created_at DESC
LIMIT 5;
```

Expected: one row with the registered user's data.

Exit: `\q`

---

## Debugging Checklist — "database does not exist"

### Root cause: init script didn't run
PostgreSQL only runs `/docker-entrypoint-initdb.d/` scripts on a **fresh empty volume**.
If the volume already existed (from a previous run with wrong DB names), the script is skipped.

**Fix:**
```bash
docker compose down -v   # -v removes volumes
docker compose up --build -d
```

### Verify init script is mounted correctly
```bash
docker exec -it postgres ls /docker-entrypoint-initdb.d/
# Must show: init-db.sql
```

### Check postgres logs for init script execution
```bash
docker logs postgres 2>&1 | grep -E "init|CREATE DATABASE|auth_db|user_db"
```

---

## Debugging Checklist — User Service Not Receiving Events

### 1. Check Kafka bootstrap-servers
```bash
docker exec user-service env | grep KAFKA
# Expected: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### 2. Check consumer group lag
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server kafka:29092 \
  --describe \
  --group user-service-group
```

### 3. Check user-service logs
```bash
docker logs user-service --tail 100 -f
```
Look for: `Received UserCreatedEvent` (success) or `ListenerExecutionFailedException` (deserialization error)

### 4. Check auth-service published the event
```bash
docker logs auth-service --tail 50 | grep "user-events"
```

### 5. Deserialization issues
If you see `JsonParseException`:
- `spring.json.add.type.headers: false` must be set on auth-service producer
- `spring.json.value.default.type: com.banking.common.event.UserCreatedEvent` must be set on user-service consumer

---

## Useful Commands

```bash
# View logs for a specific service
docker logs auth-service -f
docker logs user-service -f
docker logs postgres -f

# Rebuild a single service
docker compose up --build -d auth-service

# Full reset
docker compose down -v && docker compose up --build -d
```
