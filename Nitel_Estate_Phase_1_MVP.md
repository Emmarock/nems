# Nitel Estate Management System (NEMS) — Phase 1: Estate Management MVP

> Part of the [NEMS Roadmap](Nitel_Estate_Management_System_NEMS.md). This phase must be implemented and stabilized before starting [Phase 2 — Resident Experience](Nitel_Estate_Phase_2_Resident_Experience.md).

## Objective

Establish the core system of record for the estate: who lives here, what properties exist, what vehicles are registered, what is owed, and who administers the platform. Every later phase (resident self-service, security, ANPR) reads from this foundation instead of maintaining its own data.

```text
                    NITEL ESTATE MANAGEMENT
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   RESIDENTS              FINANCE             ADMIN
        │                    │                    │
   ┌────┴────┐         ┌─────┴─────┐        ┌─────┴─────┐
   │         │         │           │        │           │
Properties Vehicles  Dues       Payments  Users      Roles
```

## Modules in Scope

1. Residents
2. Properties
3. Vehicles
4. Estate Dues & Billing
5. Payments (admin-recorded / back-office)
6. Administration (users & roles)

Everything else in the system (resident self-service, visitor/worker management, complaints, announcements, security, ANPR) is explicitly **out of scope** for Phase 1.

---

## 1. Residents

Each resident profile contains:

- Resident ID
- Full name
- Phone
- Email
- Property/house number
- Resident type — owner, tenant, landlord, etc.
- Emergency contact
- Account status
- Registration date

The system must support relationships between landlords and tenants.

```text
Property: House 24B

Owner
 └── Mr. Adewale

Occupants
 ├── Mrs. Adewale
 ├── Child 1
 └── Child 2

Tenant
 └── None
```

**API**

```http
POST   /api/v1/residents
GET    /api/v1/residents/{id}
PUT    /api/v1/residents/{id}
```

---

## 2. Properties

The estate's property registry — the foundation for billing and resident management.

```text
Property
---------
Block
Plot
House Number
Address
Property Type
Owner
Occupancy Status
```

```text
Plot 124
   ↓
House 24B
   ↓
Owner: Mr. Adewale
   ↓
Tenant: Mrs. Smith
```

**API**

```http
POST   /api/v1/properties
GET    /api/v1/properties/{id}
```

---

## 3. Vehicles

Start collecting vehicle information now, even though ANPR is not being built yet — ANPR (Phase 4) will simply consume this existing data.

```text
Vehicle
--------
Plate Number
Vehicle Type
Make
Model
Colour
Owner/Resident
Status
```

```text
LAG-123-AB
Toyota Prado
Black

Resident:
Mr. Adewale

Status:
ACTIVE
```

**API**

```http
POST   /api/v1/vehicles
GET    /api/v1/vehicles/{plate}
```

---

## 4. Estate Dues and Billing

One of the most important modules after resident management. The CDA defines charges:

```text
Estate Levy       ₦100,000 / year
Security Levy      ₦30,000 / year
Waste Levy         ₦20,000 / year
Development Levy   ₦50,000 / year
```

The system generates the resident's account:

```text
MR ADEWALE

Estate Levy          ₦100,000
Security              ₦30,000
Waste                  ₦20,000
                    ----------
Total                ₦150,000

Paid                 ₦100,000
Outstanding           ₦50,000
```

Core calculation:

```text
TOTAL DUE
- PAYMENTS
+ PENALTIES
= OUTSTANDING
```

This outstanding balance will eventually become an input to the Phase 4 ANPR access decision.

**API**

```http
GET    /api/v1/accounts/{residentId}
GET    /api/v1/accounts/{residentId}/balance
```

---

## 5. Payments (Back-Office)

In Phase 1, payments are recorded by administrative staff (bank transfer confirmations, cash, cheque) against a resident's account — no online payment gateway yet. Self-service online payment is introduced in Phase 2 (§5) once the Resident Portal exists.

**API**

```http
POST   /api/v1/payments
GET    /api/v1/payments/{id}
```

Even at this stage, structure the payment module behind a `PaymentProvider` abstraction so a gateway (Paystack, Flutterwave, Monnify) can be plugged in later without rewriting the finance module:

```text
PaymentProvider
      │
 ┌────┼───────────┐
 │    │           │
Paystack Flutterwave Monnify
```

---

## 6. Administration

Role-based access control, scoped to what Phase 1 needs:

### Super Admin
Full access to the platform.

### CDA Administrator
Residents, properties, finances, notices.

### Treasurer
Payments, dues, financial reports.

Additional roles (Secretary, Security, Maintenance, Resident) are defined in the full roadmap but only become operationally relevant once complaints, visitor/worker management, and the resident portal ship in Phase 2.

---

## Architecture Notes (Phase 1)

- **Backend:** Spring Boot modular monolith, Java. Suggested modules for this phase:

```text
nitel-estate
│
├── resident
├── property
├── vehicle
├── billing
├── payment
├── user
└── audit
```

- **Database:** PostgreSQL.

```text
                    RESIDENT
                       │
             ┌─────────┼─────────┐
             │         │         │
             ▼         ▼         ▼
          PROPERTY   VEHICLE   ACCOUNT
                                  │
                                  ▼
                               INVOICE
                                  │
                                  ▼
                               PAYMENT
```

- Each module keeps its own `Controller / Service / Repository / Entity / DTO`, but all modules run in one deployable application.

---

## Definition of Done for Phase 1

- [ ] Residents can be created, updated, and linked to properties (owner/tenant relationships).
- [ ] Properties registry is populated and queryable.
- [ ] Vehicles can be registered against a resident.
- [ ] CDA can define levies/charges and generate resident accounts.
- [ ] Outstanding balance calculation (`TOTAL DUE - PAYMENTS + PENALTIES = OUTSTANDING`) is correct and testable.
- [ ] Admin staff can record payments against a resident's account.
- [ ] Super Admin, CDA Administrator, and Treasurer roles enforce access control.
- [ ] Audit log captures create/update actions on residents, properties, vehicles, and payments.

Once these are stable in production use, proceed to [Phase 2 — Resident Experience](Nitel_Estate_Phase_2_Resident_Experience.md).
