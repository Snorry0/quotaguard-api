# QuotaGuard API

QuotaGuard API is a domain-neutral resource usage control backend built with Java 21, Spring Boot, Spring Security JWT, Spring Data JPA, PostgreSQL, Lombok, and MapStruct.

It can be presented as:

- Enterprise quota management / API rate-limiting backend.
- Neutral user activity regulation and behavioral tracking backend.

## Main flow

1. Authenticated user requests `/api/v1/usage/consume` with an amount and action type.
2. System locks the user's quota row to avoid race conditions.
3. System performs lazy daily reset if needed.
4. Active blocking penalty prevents consumption.
5. If usage would exceed the daily limit, the penalty level progresses and a penalty event is stored.
6. If valid, usage is persisted and `usedToday` is increased.

## Run locally

```bash
docker compose up -d
./mvnw spring-boot:run
```

## Key endpoints

Base path: `/api/v1`

- `POST /auth/register`
- `POST /auth/login`
- `GET /users/me`
- `POST /usage/consume`
- `GET /usage/history`
- `GET /quota`
- `POST /quota/reset` - admin only
- `GET /penalties`
- `GET /stats/usage`
- `GET /stats/trend`

## Example requests

Register:

```json
{
  "email": "user@example.com",
  "password": "P@ssword123"
}
```

Consume resource:

```json
{
  "amountConsumed": 15,
  "actionType": "API_CALL"
}
```

Example quota response:

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

## Notes

Public registration always creates `USER` accounts to avoid privilege escalation. Create admins through controlled operational processes such as database migration, seed data, or a protected admin endpoint.
