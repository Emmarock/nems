# Nitel Estate Management System (NEMS) — Phase 3: Security

> Part of the [NEMS Roadmap](Nitel_Estate_Management_System_NEMS.md). Depends on [Phase 2 — Resident Experience](Nitel_Estate_Phase_2_Resident_Experience.md) being complete (visitor management, the Worker Module, and resident accounts must already exist). This phase lays the groundwork for [Phase 4 — Smart Gate / ANPR integration](Nitel_Estate_Management_System_NEMS.md#phase-4--smart-gate).

## Objective

Give the estate's security team dedicated tooling to manage gates, enforce access policies, and log every entry/exit — for residents, visitors, and workers alike — using the data already captured in Phases 1 and 2. This phase is deliberately software-only (dashboard, app, policies, logs); physical ANPR camera integration is Phase 4.

## Modules in Scope

1. Security Dashboard
2. Gate Management
3. Access Policies
4. Security Officer App
5. Access Logs
6. RFID (physical credential support)

---

## 1. Security Dashboard

A real-time operational view for the Security role, built on top of data already produced by earlier phases:

- Residents currently expected/registered (from Phase 1)
- Active visitor passes for today (from Phase 2 §3)
- Active workers on site, by contractor and site (from Phase 2 §4 — Worker Module)
- Residents with outstanding balances flagged per the estate's access policy (from Phase 1 §4)

```text
SECURITY DASHBOARD — TODAY

Visitors expected:        14
Workers on site:           9   (Delta Builders Ltd, JEC Electrical)
Vehicles registered:   1,672
Accounts in arrears:      63   (flagged per access policy)
```

---

## 2. Gate Management

Register and configure the estate's physical gates as entities the system can reference in access decisions and logs.

```text
Gate
-----
Gate ID
Name (e.g. MAIN-GATE-01)
Location
Type (vehicle / pedestrian)
Status (ACTIVE / INACTIVE)
```

This is the same `gateId` referenced later by the Phase 4 access-check API:

```json
{
  "plateNumber": "LAG123AB",
  "gateId": "MAIN-GATE-01"
}
```

---

## 3. Access Policies

Codify the rules Security enforces manually today, so Phase 4's automated ANPR decision has something authoritative to evaluate against.

Examples of policies to support:

- Deny/flag residents with outstanding balances beyond a threshold.
- Allow active visitor passes only within their booked time window.
- Allow worker access only while `Status = ACTIVE` and within `Start Date`–`Expected End Date`.
- Restrict specific gates to specific credential types (e.g. pedestrian gate accepts RFID + visitor QR, main gate accepts vehicle plates).

```text
Camera/credential detected
       ↓
Vehicle / Resident / Visitor / Worker lookup
       ↓
Account & pass status
       ↓
Access Policy
       ↓
ALLOW / DENY
```

Note: Phase 3 policies are evaluated by security officers using the dashboard/app (manual decision, system-assisted). Phase 4 is what makes this fully automatic.

---

## 4. Security Officer App

A lightweight interface for officers physically stationed at gates to:

- Look up a resident, visitor pass, or worker pass by name/plate/QR code.
- Record entry/exit (check-in/check-out), extending the same log structure introduced for workers in Phase 2 §4.
- View flags raised by Access Policies (e.g. "account in arrears", "worker pass expired") before waving someone through.

**API**

```http
POST   /api/v1/access-events
GET    /api/v1/access-events/{id}
POST   /api/v1/workers/{id}/checkin      (from Phase 2, now used operationally by this app)
POST   /api/v1/workers/{id}/checkout
```

---

## 5. Access Logs

Every entry/exit — resident, visitor, or worker — is recorded to a unified `ACCESS_EVENT` log:

```text
                    RESIDENT
                       │
             ┌─────────┼─────────┐
             │         │         │
             ▼         ▼         ▼
          PROPERTY   VEHICLE   ACCOUNT
                       │
                       ▼
                 ACCESS_EVENT
                       │
                       ▼
                    ANPR   (Phase 4)
```

Reports to add in this phase:

- Daily/weekly access log by gate
- Access denials and the policy that triggered them
- Worker attendance history (site, contractor, hours on site)
- Visitor traffic patterns

---

## 6. RFID

Introduce RFID tags/cards as a physical credential option for residents and long-term workers, ahead of full ANPR:

```text
RFID Tag
---------
Tag ID
Assigned To (Resident / Worker)
Vehicle (optional)
Status (ACTIVE / LOST / REVOKED)
```

This gives Security a working automated-ish credential (tap-to-verify) before camera-based plate recognition (Phase 4) is built, and validates the `ACCESS_EVENT` pipeline end-to-end with real hardware.

---

## Roles Affected

### Security
Full operational owner of this phase: visitors, workers, vehicles, access events, gates.

### Super Admin / CDA Administrator
Configure gates and access policies.

### Treasurer
Supplies the arrears data that access policies key off of (via the Phase 1 Finance module).

---

## Architecture Notes (Phase 3)

```text
nitel-estate
│
├── resident         (Phase 1)
├── property          (Phase 1)
├── vehicle           (Phase 1)
├── billing           (Phase 1)
├── payment           (Phase 1/2)
├── visitor           (Phase 2)
├── worker            (Phase 2)
├── complaint         (Phase 2)
├── notification      (Phase 2)
├── announcement      (Phase 2)
├── user              (Phase 1)
├── audit             (Phase 1)
├── reporting         (Phase 2, extended)
├── gate              ← new
├── access-policy     ← new
└── access-event       ← new (unifies visitor/worker/resident logs)
```

Key principle carried into this phase: **Security does not maintain its own resident/visitor/worker database** — it reads from the modules built in Phases 1–2 and only adds gate, policy, and event-logging concerns.

---

## Definition of Done for Phase 3

- [ ] Gates are registered and referenceable by ID.
- [ ] Access policies can be configured (arrears threshold, visitor/worker time-window enforcement).
- [ ] Security Dashboard shows live visitor, worker, and arrears data.
- [ ] Security Officer App supports lookup and check-in/check-out for residents, visitors, and workers, writing to a unified `ACCESS_EVENT` log.
- [ ] RFID credentials can be issued, verified, and revoked.
- [ ] Access log reporting (by gate, by denial reason, by worker attendance) is available.

Once these are stable in production use, the estate is ready to integrate ANPR and automated gate control (Phase 4), per the [NEMS Roadmap](Nitel_Estate_Management_System_NEMS.md#phase-4--smart-gate).
