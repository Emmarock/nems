# Nitel Estate Management System (NEMS) — Phase 2: Resident Experience

> Part of the [NEMS Roadmap](Nitel_Estate_Management_System_NEMS.md). Depends on [Phase 1 — Estate Management MVP](Nitel_Estate_Phase_1_MVP.md) being complete (residents, properties, vehicles, dues, and accounts must already exist). Must be complete before starting [Phase 3 — Security](Nitel_Estate_Phase_3_Security.md).

## Objective

Turn the back-office system built in Phase 1 into something residents, contractors, and estate admins interact with directly: a self-service portal, online payments, visitor and worker access requests, complaints, announcements, notifications, and reporting.

## Modules in Scope

1. Resident Portal
2. Online Payments
3. Visitor Management
4. **Worker Module** *(added — see rationale below)*
5. Complaints / Service Requests
6. Estate Announcements
7. Notifications (SMS / WhatsApp / Email)
8. Reporting

---

## 1. Resident Portal

Residents get their own dashboard.

```text
┌─────────────────────────────────┐
│       NITEL ESTATE              │
│                                 │
│ Welcome, Mr. Adewale            │
│                                 │
│ ACCOUNT                         │
│ Outstanding: ₦50,000            │
│                                 │
│ [ PAY NOW ]                     │
│                                 │
│ PROPERTY                        │
│ House 24B                       │
│                                 │
│ VEHICLES                        │
│ LAG-123-AB                      │
│ KJA-456-XY                      │
│                                 │
│ [ Register Vehicle ]            │
│                                 │
│ VISITORS                        │
│ [ Create Visitor Pass ]         │
│                                 │
│ WORKERS                         │
│ [ Request Worker Access ]       │
│                                 │
│ NOTICES                         │
│ • Security meeting – Saturday   │
└─────────────────────────────────┘
```

The portal is the delivery surface for everything else in this phase — online payments, visitor passes, worker access requests, complaints, and notices all surface here.

---

## 2. Online Payments

Residents pay their estate dues online instead of relying on admin-recorded payments (Phase 1 §5).

```text
Resident
   ↓
Resident Portal
   ↓
"Pay Outstanding"
   ↓
Payment Gateway
   ↓
Payment Confirmation
   ↓
Webhook
   ↓
NEMS
   ↓
Account Updated
```

Potential Nigerian payment providers:

- Paystack
- Flutterwave
- Monnify

This plugs into the `PaymentProvider` abstraction already scaffolded in Phase 1, so adding/switching providers doesn't require rewriting the finance module.

---

## 3. Visitor Management

Short-term, one-off visitors (guests, deliveries, service calls), distinct from the recurring on-site workers covered in §4.

A resident creates a visitor pass:

```text
Visitor

Name: John Smith
Phone: 080...
Vehicle: ABC-123-XY

Date: 5 September
From: 4:00 PM
Until: 10:00 PM
```

The system generates a QR-based visitor pass:

```text
VISITOR PASS
      │
      ▼
   QR CODE
```

Later, when gate access (Phase 3/4) is introduced:

```text
QR visitor pass
       +
ANPR
       +
Resident status
       ↓
Gate decision
```

**API**

```http
POST   /api/v1/visitors
GET    /api/v1/visitors/{id}
```

---

## 4. Worker Module *(new)*

### Why this is in Phase 2

Nitel Estate is still under active development — construction, landscaping, and utility work are ongoing, so labourers and contractors come on site regularly. They don't fit the Visitor Management model above: a visitor pass is a single short time window, but a worker typically needs **repeated, multi-day access tied to a specific contractor and site**, sponsored by a resident or the estate developer, and subject to approval/expiry/suspension. Building this alongside Visitor Management (rather than deferring it to Phase 3 Security) means the estate has proper on-site labour tracking from the moment the resident-facing portal goes live, instead of relying on ad-hoc gate sign-in sheets.

### Data Model

```text
Worker
--------
Worker ID
Full Name
Phone
National ID / Work ID
Contractor / Company Name
Type of Work (construction, plumbing, electrical, landscaping, etc.)
Assigned Site (Block / Plot / Property)
Sponsor (Resident or Estate Developer who requested access)
Start Date
Expected End Date
Status (PENDING, APPROVED, ACTIVE, SUSPENDED, EXPIRED, COMPLETED)
```

Example:

```text
Worker: Musa Ibrahim
Company: Delta Builders Ltd
Work: Construction — foundation

Assigned Site:
Plot 124, House 24B

Sponsor:
Mr. Adewale

Access Window:
1 Sept – 30 Nov

Status:
ACTIVE
```

### Worker Access Pass

Same QR-based mechanism as the Visitor Pass, but scoped to a **date range** rather than a single time window, and renewable without re-issuing a new pass:

```text
WORKER ACCESS PASS
      │
      ▼
   QR CODE  (valid: Start Date → Expected End Date)
```

### Approval Workflow

```text
REQUESTED
   ↓
APPROVED         (CDA Administrator / Estate Manager sign-off)
   ↓
ACTIVE           (worker is on the current access list)
   ↓
COMPLETED / EXPIRED / SUSPENDED
```

A resident (or the estate developer, for common-area works) submits a worker access request through the Resident Portal; a CDA Administrator approves it before the pass becomes usable at the gate.

### Entry/Exit Logging

```text
Worker Log
-----------
Worker
Gate
Time In
Time Out
Verified By (security officer)
```

This log is captured now (manually, by whoever staffs the gate) so that when Phase 3's Security module and Phase 4's ANPR integration arrive, worker check-in/out becomes an automatic extension of the same access-event pipeline used for visitors and residents — no separate system to bolt on later.

### API

```http
POST   /api/v1/workers
GET    /api/v1/workers/{id}
PUT    /api/v1/workers/{id}
POST   /api/v1/workers/{id}/approve
POST   /api/v1/workers/{id}/access-pass
POST   /api/v1/workers/{id}/checkin
POST   /api/v1/workers/{id}/checkout
GET    /api/v1/workers/{id}/logs
```

### Roles Affected

- **CDA Administrator** — approves worker access requests.
- **Security** — gains "Workers" alongside "Visitors, Vehicles" as part of their access-event scope (operationally exercised once Phase 3's Security Dashboard exists, but the data/API must exist now).
- **Resident** — can request worker access for their property.

---

## 5. Complaints / Service Requests

Residents report:

- Electricity issues
- Water issues
- Security concerns
- Waste disposal
- Road problems
- Drainage
- Streetlight issues
- General complaints

```text
Complaint #REQ-001293

Category:
Electricity

Description:
Street light not working on Block C.

Resident:
Mr. Adewale

Status:
ASSIGNED

Assigned to:
Estate Maintenance

Priority:
HIGH
```

Workflow:

```text
OPEN
 ↓
ASSIGNED
 ↓
IN PROGRESS
 ↓
RESOLVED
 ↓
CLOSED
```

**API**

```http
POST   /api/v1/complaints
GET    /api/v1/complaints/{id}
```

---

## 6. Estate Announcements

Administrators send estate-wide notices, e.g.:

> Water supply will be interrupted on Saturday from 10am–2pm.

Distributed through:

- Resident portal
- Email
- SMS
- WhatsApp
- Push notifications later

---

## 7. Notifications

```text
NOTIFICATIONS
     │
┌────┴────┐
│         │
SMS    WhatsApp
```

Used to deliver announcements, payment confirmations, complaint status updates, and — new in this phase — worker access approvals/expiry warnings to the sponsoring resident.

---

## 8. Reporting

Dashboards for the CDA, extended in this phase with resident-experience metrics:

```text
NITEL ESTATE — SEPTEMBER 2026

Residents                 1,284
Properties                1,150
Registered Vehicles       1,672

TOTAL BILLING
₦185,000,000

COLLECTED
₦142,500,000

OUTSTANDING
₦42,500,000

COLLECTION RATE
77.0%
```

Reports in scope for Phase 2:

- Outstanding residents
- Monthly / annual collections
- Payment history
- Residents by property
- Occupancy
- Registered vehicles
- Complaints
- Visitor activity
- **Active workers on site** *(new — supports the Worker Module)*

---

## Architecture Notes (Phase 2)

Extend the modular monolith with:

```text
nitel-estate
│
├── resident        (from Phase 1)
├── property         (from Phase 1)
├── vehicle          (from Phase 1)
├── billing          (from Phase 1)
├── payment          (extended: online gateway integration)
├── visitor
├── worker           ← new
├── complaint
├── notification
├── announcement
├── user             (from Phase 1)
├── audit            (from Phase 1)
└── reporting
```

The `worker` module should mirror the shape of `visitor` (pass generation, QR code, status lifecycle) but with an added approval step and a date-range validity instead of a single time window, since it will later share the same access-event log used by Security (Phase 3) and ANPR (Phase 4).

---

## Definition of Done for Phase 2

- [ ] Residents can log into a portal and view account, property, and vehicles.
- [ ] Residents can pay outstanding dues online via at least one payment gateway, with webhook-driven account updates.
- [ ] Residents can create visitor passes with QR codes.
- [ ] Residents/developer can request worker access; CDA Administrator can approve; pass is issued with a valid date range; entry/exit is logged.
- [ ] Residents can file complaints and track status through the OPEN → CLOSED workflow.
- [ ] Admins can broadcast announcements via portal, SMS, WhatsApp, and email.
- [ ] Reporting dashboard includes worker and visitor activity alongside financial metrics.

Once these are stable in production use, proceed to [Phase 3 — Security](Nitel_Estate_Phase_3_Security.md).
