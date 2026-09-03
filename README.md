# Nitel Estate Management System (NEMS)

Spring Boot + PostgreSQL backend and a React SPA frontend implementing Phases 1–3 of the
[NEMS roadmap](Nitel_Estate_Management_System_NEMS.md):

- [Phase 1 — Estate Management MVP](Nitel_Estate_Phase_1_MVP.md)
- [Phase 2 — Resident Experience](Nitel_Estate_Phase_2_Resident_Experience.md) (includes the **Worker Module**)
- [Phase 3 — Security](Nitel_Estate_Phase_3_Security.md)

See [`.claude/plans`](../.claude/plans) history or the phase docs above for the full design. In short:
payments and SMS/WhatsApp notifications are stubbed (`Mock*` implementations that log instead of
calling a real provider) so the app runs with zero external credentials — swap in a real
`PaymentProvider`/`NotificationSender` implementation later without touching calling code.

## Prerequisites

- Java 21, Docker (for Postgres), Node.js 18+
- No global Maven needed — use the bundled `./mvnw`

## Running it

```bash
# 1. Start Postgres
docker compose up -d

# 2. Start the backend (http://localhost:8080)
./mvnw spring-boot:run

# 3. Start the frontend (http://localhost:5173, proxies /api to the backend)
cd frontend
npm install
npm run dev
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Seeded accounts (password: `Passw0rd!`)

| Email                        | Role         |
|-------------------------------|--------------|
| admin@nitelestate.local       | SUPER_ADMIN  |
| cda@nitelestate.local         | CDA_ADMIN    |
| treasurer@nitelestate.local   | TREASURER    |
| security@nitelestate.local    | SECURITY     |
| resident@nitelestate.local    | RESIDENT (Mr. Adewale, House 24B) |

## Tests

```bash
./mvnw test        # unit tests + one Testcontainers-backed integration test (needs Docker running)
cd frontend && npx tsc -b && npm run build   # type-check + production build
```

## Project layout

```
src/main/java/com/cyrev/nitelestate/
  user, security, auth        Admin/auth (Phase 1)
  resident, property, vehicle, billing, payment, audit    Phase 1
  access, visitor, worker, complaint, announcement,
  notification, reporting, portal                          Phase 2
  gate, accesspolicy, rfid, estatesecurity                  Phase 3
src/main/resources/db/migration/  Flyway migrations, one file per phase + dev seed data
frontend/src/
  api/        typed API client (axios) per backend module
  auth/       JWT auth context + role-gated routes
  components/ generic DataTable/FormModal/Layout used across all admin screens
  pages/auth, pages/admin, pages/portal, pages/security     mirrors the 3 phases + resident/staff/security views
```
