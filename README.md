# QuotaGuard API

QuotaGuard API is a domain-neutral resource usage control backend built with Java 21 and Spring Boot.

It tracks user resource consumption, enforces dynamic daily quotas, applies progressive penalties, stores usage history, and exposes analytics for behavior-aware systems.

The project can be presented as:

- an enterprise quota management backend
- an API rate-limiting service
- a SaaS usage control system
- a neutral user activity regulation backend
- a session-aware resource tracking platform

## API Documentation

Full interactive API documentation: `/swagger-ui.html` (the OpenAPI 3.0 spec is also exposed at `/v3/api-docs`). The Swagger UI includes authentication support (Authorize button for the JWT bearer token), realistic request/response examples, and per-endpoint error documentation. This README is a quickstart summary; for the full contract, use Swagger.

Validation constraints are documented per-field inline in Swagger. See the [Authentication](#authentication) section below and [docs/08-validation.md](docs/08-validation.md) for the validation reference.

## Purpose

QuotaGuard is not a simple CRUD application.

The goal of the project is to demonstrate backend engineering concepts such as:

- stateful business logic
- quota enforcement
- progressive penalty handling
- authentication and authorization
- database-backed usage history
- scheduled reset logic
- analytics endpoints
- clean layered architecture
- production-oriented project structure

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Security
- JWT Authentication
- Spring Data JPA
- PostgreSQL
- Liquibase
- Lombok
- MapStruct
- Docker Compose
- Swagger / OpenAPI
- Bruno API Client

## Architecture

The project follows a layered architecture:

```text
controller
   ↓
service
   ↓
repository
   ↓
domain
```

Supporting packages:

```text
config
dto
exception
mapper
security
```

This keeps HTTP transport, business rules, persistence, mapping, configuration, and security concerns separated.

## Core Domain

### User

Represents an authenticated user of the system.

Fields include:

- id
- email
- password hash
- role
- created date

### UserQuota

Represents the current quota state of a user.

It tracks:

- daily limit
- usage consumed today
- last reset date
- penalty level

### UsageRecord

Represents a single consumption event.

Each record stores:

- user
- consumed amount
- action type
- timestamp

### PenaltyEvent

Represents active or historical penalties.

Penalties are used to apply progressive friction when users exceed their quota.

### UsageSession

Represents a session lifecycle.

A session can be started and ended. When ended, its duration can be converted into resource consumption.

This makes the system suitable for session-based use cases without making the API domain-specific.

## Main Usage Flow

1. Authenticated user sends a request to `/api/v1/usage/consume`.
2. System retrieves and locks the user's quota row to avoid race conditions.
3. System performs a lazy daily reset if the quota date is outdated.
4. System checks whether the user has an active blocking penalty.
5. System checks whether the requested usage would exceed the daily limit.
6. If the request is valid:
    - usage is persisted
    - `usedToday` is increased
    - updated quota state is returned
7. If the request exceeds the daily limit:
    - penalty level increases
    - a penalty event may be created
    - the request is rejected with a structured error response

## Penalty Model

QuotaGuard uses progressive penalties instead of a single hard failure mode.

Example strategy:

```text
penalty level 1  -> warning
penalty level 2  -> short cooldown
penalty level 3+ -> long cooldown
```

This makes the system extensible for multiple domains, including rate limiting, credit systems, SaaS usage restriction, and session regulation.

## Authentication

### Obtaining a token

`POST /api/v1/auth/login` with the body:

```json
{
  "email": "demo@example.com",
  "password": "Password123!"
}
```

Returns `200` with:

```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_at": "...",
  "user": { ... }
}
```

### Using the token

Subsequent requests include the header on every authenticated request:

```text
Authorization: Bearer <access_token>
```

### Expiration and claims

Tokens expire after 12 hours by default (configurable via the `JWT_EXPIRATION_HOURS` environment variable / `security.jwt.expiration` property).

The JWT contains `sub` (user identifier) and `role` (`USER` or `ADMIN`). Tokens are bound to the user identifier, NOT the email — changing a user's email does NOT invalidate their active tokens.

### Swagger UI authentication

In the Swagger UI, click the "Authorize" button (top right) and paste the access token (the `Bearer ` prefix is added automatically by Swagger). The token persists across "Try it out" reloads (`persist-authorization: true` in the springdoc configuration).

### Public vs authenticated

`POST /api/v1/auth/register` and `POST /api/v1/auth/login` are public; all other endpoints require a valid token. Admin endpoints (`/api/v1/users` admin operations, `/api/v1/quota/reset`, `/api/v1/audit/**`) additionally require the `ADMIN` role — see the "Security" note in each endpoint's Swagger description.

## Observability

The application exposes operational health and metrics through Spring Boot Actuator + Micrometer. See [docs/09-observability.md](docs/09-observability.md) for the full reference.

### Actuator endpoints

| Endpoint | Exposure | Returns |
|---|---|---|
| `GET /actuator/health` | Public | `{"status":"UP","components":{...}}` — the `db`, `diskSpace` and `quotaGuardConfig` components; the liveness/readiness probes at `/actuator/health/liveness` and `/actuator/health/readiness` are also public (k8s convention) |
| `GET /actuator/info` | Public | Build/env info |
| `GET /actuator/metrics` | Authenticated | Index of metric names; `GET /actuator/metrics/{name}` returns the metric's details + samples |
| `GET /actuator/prometheus` | Authenticated | Scrape in Prometheus text format 0.0.4 |

### Security model

`/actuator/health` and `/actuator/info` are configured for unauthenticated access (liveness/readiness probes for k8s). `/actuator/metrics` and `/actuator/prometheus` require a valid JWT (any authenticated user).

**Production recommendation:** restrict `/actuator/metrics` and `/actuator/prometheus` to the ADMIN role (or a dedicated monitoring service token) via the `SecurityConfig` `requestMatchers` chain. This is a documented recommendation per the design's "no behaviour changes" scope — it is NOT applied in this commit.

### Business metrics reference

| Metric | Type | Tags | Counts | Source |
|---|---|---|---|---|
| `quotaguard.registrations.successful` | Counter | — | Successful user registrations | `UserRegisteredEvent` |
| `quotaguard.registrations.failed` | Counter | — | Failed user registrations | `RegisterFailedEvent` |
| `quotaguard.logins.successful` | Counter | — | Successful logins | `LoginSucceededEvent` |
| `quotaguard.logins.failed` | Counter | — | Failed logins | `LoginFailedEvent` |
| `quotaguard.quota.consumptions` | Counter | `actionType=` | Quota consumption events | `UsageConsumedEvent` |
| `quotaguard.quota.resets` | Counter | `type=daily\|bulk` | Quota resets | `QuotaResetEvent` (daily) / `BulkQuotaResetEvent` (bulk) |
| `quotaguard.penalties.applied` | Counter | `type=` | Penalties applied | `PenaltyAppliedEvent` |
| `quotaguard.penalties.expired` | Counter | — | Penalties expired | `PenaltyExpiredEvent` |
| `quotaguard.sessions.active` | Gauge | — | Currently active usage sessions | Repository query (per scrape) |
| `quotaguard.sessions.completed` | Counter | — | Usage sessions completed | `SessionCompletedEvent` |
| `quotaguard.sessions.completion.failed` | Counter | — | Failed usage session completions | `UsageSessionService.endSession` catch blocks (no event) |
| `quotaguard.admin.operations` | Counter | `type=` | Manual admin operations | `UserCreatedEvent` / `UserUpdatedEvent` / `UserDeletedEvent` / `BulkQuotaResetEvent` (non-SYSTEM actor) |

Full details — every metric including the 5 timers (`quotaguard.timer.registration`, `quotaguard.timer.login`, `quotaguard.timer.quota.consumption`, `quotaguard.timer.session.completion`, `quotaguard.timer.quota.reset`) — live in [docs/09-observability.md](docs/09-observability.md).

### Prometheus future scraping

```yaml
scrape_configs:
  - job_name: quotaguard
    metrics_path: /actuator/prometheus
    scheme: http
    static_configs:
      - targets: ["localhost:8080"]
    # If /actuator/metrics and /actuator/prometheus are behind auth:
    authorization:
      type: Bearer
      credentials_file: /etc/quotaguard/monitoring.token
```

Scraped metric names follow the Prometheus convention: dots become underscores, counters get a `_total` suffix, and timers get a `_seconds` base plus `_count` / `_max` / `_sum` series.

For the full metric reference, custom health indicator, and production security recommendations, see `docs/09-observability.md`.

## API Endpoints

Base path:

```text
/api/v1
```

These tables are a quick-reference summary. For the full contract (every request/response field, every error code, every validation constraint, realistic examples), use the Swagger UI at `/swagger-ui.html`.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Authenticate and receive JWT |

### Users

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/me` | Get the authenticated user's profile |
| GET | `/users` | List users, admin only |
| GET | `/users/{userId}` | Get a user, admin only |
| POST | `/users` | Create a user, admin only |
| PATCH | `/users/{userId}` | Update a user partially, admin only |
| DELETE | `/users/{userId}` | Delete a user, admin only |

### Usage

| Method | Endpoint | Description |
|---|---|---|
| POST | `/usage/consume` | Consume resource units |
| GET | `/usage/history` | Get usage history |

### Quota

| Method | Endpoint | Description |
|---|---|---|
| GET | `/quota` | Get current quota state |
| POST | `/quota/reset` | Reset quotas manually, admin only |

### Penalties

| Method | Endpoint | Description |
|---|---|---|
| GET | `/penalties` | Get user penalty events |

### Analytics

| Method | Endpoint | Description |
|---|---|---|
| GET | `/stats/usage` | Get usage statistics |
| GET | `/stats/trend` | Get usage trend data |

### Sessions

| Method | Endpoint | Description |
|---|---|---|
| POST | `/sessions/start` | Start a tracked usage session |
| POST | `/sessions/{sessionId}/end` | End a session and consume quota |
| GET | `/sessions/active` | Get the current active session |
| GET | `/sessions/history` | Get session history |

### Audit

| Method | Endpoint | Description |
|---|---|---|
| GET | `/audit` | List audit events, admin only, paginated and sortable |
| GET | `/audit/{eventId}` | Get a single audit event, admin only |

The audit trail records important business actions (user lifecycle, authentication, quota resets, penalties, sessions). Audit persistence is isolated from business logic: a failing audit write never fails the business operation, and successful actions are recorded only after the operation commits.

## Running Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

## Database Migrations

Liquibase manages the database schema.

Changelog files are located in:

```text
src/main/resources/db/changelog
```

Hibernate is configured to validate the schema instead of generating it automatically.

This keeps schema evolution explicit and version-controlled.

## API Testing

A Bruno collection is included in the repository under:

```text
bruno/
```

Recommended test flow:

```text
Register
Login
Authorize with JWT
Get current user
Get quota
Consume usage
Check usage history
Check penalties
Check analytics
```

## Example Requests

### Register

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

### Login

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

### Consume Resource

```json
{
  "amountConsumed": 15,
  "actionType": "API_CALL"
}
```

Valid action types include:

```text
API_CALL
RESOURCE_ACCESS
BACKGROUND_JOB
SESSION_ACTION
MANUAL_ADJUSTMENT
```

### Start Session

```json
{
  "clientReference": "desktop-client"
}
```

### End Session

```json
{
  "amountConsumed": 20
}
```

If `amountConsumed` is omitted, the backend can calculate consumption from session duration.

If the end-of-session consumption is rejected (quota exceeded or active penalty), the session remains active and can be ended again later.

## Example Quota Response

```json
{
  "id": "0f8b5201-8f39-4ea8-9c52-1e67ef5d00e1",
  "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
  "dailyLimit": 100,
  "usedToday": 35,
  "remainingToday": 65,
  "lastResetDate": "2026-05-25",
  "penaltyLevel": 0
}
```

## Error Handling

The API returns structured JSON error responses.

Example validation error (HTTP 400 on `POST /api/v1/auth/register`):

```json
{
  "timestamp": "2026-08-02T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "validationErrors": {
    "email": "Email must be provided in normalized form: trimmed, lowercase and a well-formed address",
    "password": "Password must be between the configured minimum and maximum length and must satisfy all configured character-class requirements"
  },
  "errors": [
    { "field": "email", "rejectedValue": "Demo@Example.COM", "message": "Email must be provided in normalized form: trimmed, lowercase and a well-formed address" },
    { "field": "password", "rejectedValue": "password", "message": "Password must be between the configured minimum and maximum length and must satisfy all configured character-class requirements" }
  ]
}
```

`validationErrors` is the legacy field→message map; `errors` is the per-field detail list with the rejected value. Both are populated on validation failures (HTTP 400); only the base fields (`timestamp`/`status`/`error`/`message`/`path`) are populated on non-validation errors.

## Environment Variables

Example configuration is provided in:

```text
.env.example
```

Important variables:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_HOURS
DEFAULT_DAILY_LIMIT
PENALTY_DECAY_PER_RESET
SHORT_COOLDOWN
LONG_COOLDOWN
SESSION_UNITS_PER_MINUTE
SESSION_MINIMUM_CHARGE
```

Do not commit real secrets.

## Running Tests

```bash
./mvnw clean test
```

On Windows PowerShell:

```powershell
.\mvnw clean test
```

## Security Notes

Public registration always creates `USER` accounts to avoid privilege escalation.

Admins cannot delete their own account.

JWT tokens are bound to the user id, not the email address, so changing a user's email does not invalidate their active tokens.

Admin users should be created through controlled operational processes such as:

- database migration
- seed data
- secure admin-only endpoint
- manual database operation in local development

JWT secrets and database credentials must be provided through environment variables outside version control.

## Design Philosophy

QuotaGuard is intentionally domain-neutral.

The core model abstracts resource control into:

```text
usage
quota
penalty
session
analytics
```

This allows the backend to be reused across different domains without coupling the code to one specific business case.

The system focuses on adaptive control rather than simple request blocking. It is designed to apply progressive friction, preserve historical context, and expose usage patterns through analytics.

## Future Improvements

Planned improvements include:

- Redis-backed distributed quota tracking
- configurable penalty policies
- scheduled penalty decay
- WebSocket live quota updates
- event-driven analytics
- notification system
- admin dashboard
- integration tests with Testcontainers
- richer OpenAPI documentation
- frontend or dashboard client
