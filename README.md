# QuotaGuard API

QuotaGuard API is a domain-neutral resource usage control backend built with Java 21 and Spring Boot.

It tracks user resource consumption, enforces dynamic daily quotas, applies progressive penalties, stores usage history, and exposes analytics for behavior-aware systems.

The project can be presented as:

- an enterprise quota management backend
- an API rate-limiting service
- a SaaS usage control system
- a neutral user activity regulation backend
- a session-aware resource tracking platform

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

## API Endpoints

Base path:

```text
/api/v1
```

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Authenticate and receive JWT |

### Users

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/me` | Get the authenticated user's profile |

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

## API Documentation

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

JWT-protected endpoints can be tested directly from Swagger by using the `Authorize` button and providing a valid access token.

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

Example validation error:

```json
{
  "timestamp": "2026-05-25T19:43:51.963233Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/usage/consume",
  "validationErrors": {
    "amountConsumed": "must be greater than 0",
    "actionType": "must not be null"
  }
}
```

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
- GitHub Actions CI
- integration tests with Testcontainers
- richer OpenAPI documentation
- frontend or dashboard client