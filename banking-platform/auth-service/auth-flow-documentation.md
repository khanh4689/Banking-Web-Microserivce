# Banking Microservices - Authentication Flow & JWT Lifecycle

This document describes the flow of authentication within our production-ready Authentication Service.

## 1. Authentication Flow

### A. User Registration
1. Client POSTs to `/auth/register` with `username`, `email`, and `password`.
2. `AuthService` validates uniqueness and creates a new `User` entity (hashed password with `BCrypt`, roles assigned).
3. `EmailVerificationService` generates a unique token and saves it to the `email_verification_tokens` DB table.
4. An email is sent out with a verification link containing the token.

### B. Email Verification
1. User clicks the email link, sending a POST or GET request to `/auth/verify-email` with their token.
2. The service checks token validity/expiration against the DB.
3. If valid, the `User` is marked as `emailVerified = true` and `enabled = true`.

### C. Login
1. Client POSTs to `/auth/login` with `username` and `password`.
2. Spring Security's `AuthenticationManager` uses our `CustomUserDetailsService` to fetch the user and `BCryptPasswordEncoder` to verify credentials.
3. If auth fails, `401 Unauthorized` is returned.
4. If successful, `JwtUtil` signs a new `access_token` (JWT RS256).
5. `RefreshTokenService` creates a new `refresh_token` (random secure string), hashes it with SHA-256, stores the hash in the DB, and returns the raw token to the client.

## 2. JWT Lifecycle

### A. Access Token (JWT)
- **Generation:** Made during successful `/login` or `/refresh`. Signed with RSA private key.
- **Payload:** Contains `sub` (username), `roles` array, `iat`, `exp` (usually short-lived, ~15 mins).
- **Validation:** Stateless. API Gateway downloads the public key from `auth-service`'s `/.well-known/jwks.json` and verifies the signature. The API Gateway then enforces authorization rules based on the embedded `roles`.
- **Termination:** Access tokens cannot be revoked directly since they are stateless. They simply expire over time.

### B. Refresh Token Rotation
- **Generation:** Returned securely to the client during login alongside the JWT. It is long-lived (~7 days).
- **Storage:** Stored in DB as a one-way `SHA-256` hash.
- **Rotation:**
  1. Once the JWT expires, client POSTs `/auth/refresh` with the raw refresh token.
  2. Service hashes the token and finds it in the DB.
  3. If old, revoked, or expired, request is denied.
  4. If valid, the *old* token is proactively marked as `revoked = true` in the DB.
  5. A *new* refresh token + new JWT pair is generated and returned to client.
- **Security (Stolen Token Detection):** If an attacker steals a valid refresh token but the legitimate user uses it *first* (during rotation), the system detects "Refresh Token Reuse" (because the old DB token is already marked revoked). The service immediately revokes *ALL* tokens for that user, locking everyone out until a new password login occurs.
