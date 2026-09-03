# Nitel Estate Management System (NEMS)

## Overview

Yes. You do not need to build the entire ANPR and gate-access system from scratch immediately.

A better approach is to first build a solid **Nitel Estate Management System (NEMS)** that handles residents, properties, vehicles, dues, payments, complaints, notices, visitors, and administration.

Once that foundation is stable, ANPR and gate access can be added as integrations rather than being the core of the application.

---

## Phase 1 — Estate Management Platform

The MVP should be organized around these core modules:

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
   │         │         │           │        │           │
   └─────────┴─────────┴───────────┴────────┴───────────┘
                             │
                             ▼
                    NOTIFICATIONS
                             │
                    ┌────────┴────────┐
                    │                 │
                  SMS              WhatsApp
```

## 1. Residents

Each resident should have a profile containing:

- Resident ID
- Full name
- Phone
- Email
- Property/house number
- Resident type — owner, tenant, landlord, etc.
- Emergency contact
- Account status
- Registration date

The system should support relationships between landlords and tenants.

Example:

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

---

## 2. Properties

Create the estate's property registry.

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

Example:

```text
Plot 124
   ↓
House 24B
   ↓
Owner: Mr. Adewale
   ↓
Tenant: Mrs. Smith
```

This property registry becomes the foundation for billing and resident management.

---

## 3. Vehicles

Even before ANPR is introduced, start collecting vehicle information.

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

Example:

```text
LAG-123-AB
Toyota Prado
Black

Resident:
Mr. Adewale

Status:
ACTIVE
```

Later, ANPR simply consumes this existing data.

---

## 4. Estate Dues and Billing

This should be one of the most important modules after resident management.

The CDA should be able to define charges such as:

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

The basic calculation should be:

```text
TOTAL DUE
- PAYMENTS
+ PENALTIES
= OUTSTANDING
```

This outstanding balance will eventually become one of the inputs to the ANPR access decision.

---

## 5. Online Payments

Eventually, residents should be able to pay their estate dues online.

The flow should be:

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

Potential Nigerian payment providers include:

- Paystack
- Flutterwave
- Monnify

The payment module should not be tightly coupled to a single provider.

Instead, create a provider abstraction:

```text
PaymentProvider
      │
 ┌────┼───────────┐
 │    │           │
Paystack Flutterwave Monnify
```

This makes it possible to change or add payment providers without rewriting the finance module.

---

## 6. Resident Portal

Residents should eventually have their own dashboard.

Example:

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
│ NOTICES                         │
│ • Security meeting – Saturday   │
└─────────────────────────────────┘
```

---

## 7. Complaints / Service Requests

Residents should be able to report:

- Electricity issues
- Water issues
- Security concerns
- Waste disposal
- Road problems
- Drainage
- Streetlight issues
- General complaints

Example:

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

Recommended workflow:

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

---

## 8. Estate Announcements

Administrators should be able to send estate-wide notices.

Example:

> Water supply will be interrupted on Saturday from 10am–2pm.

The system can distribute announcements through:

- Resident portal
- Email
- SMS
- WhatsApp
- Push notifications later

---

## 9. Visitor Management

Visitor management should be built before ANPR.

A resident can create a visitor pass:

```text
Visitor

Name: John Smith
Phone: 080...
Vehicle: ABC-123-XY

Date: 5 September
From: 4:00 PM
Until: 10:00 PM
```

The system can generate a QR-based visitor pass:

```text
VISITOR PASS
      │
      ▼
   QR CODE
```

Later, when gate access is introduced:

```text
QR visitor pass
       +
ANPR
       +
Resident status
       ↓
Gate decision
```

This means the existing visitor-management system can integrate directly with the future gate system.

---

## 10. Administration

Create role-based access control.

### Super Admin

Full access to the platform.

### CDA Administrator

Residents, properties, finances, notices.

### Treasurer

Payments, dues, financial reports.

### Secretary

Residents, notices, complaints.

### Security

Visitors, vehicles, access events.

### Maintenance

Service requests.

### Resident

Access only to their own information and permitted estate services.

---

## 11. Reporting

The CDA should have dashboards such as:

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

Useful reports include:

- Outstanding residents
- Monthly collections
- Annual collections
- Payment history
- Residents by property
- Occupancy
- Registered vehicles
- Complaints
- Visitor activity

---

# Recommended Architecture

Given the existing technical background, the recommended starting point is a **modular monolith**, rather than immediately building microservices.

## Backend

Use **Spring Boot + Java**.

Suggested structure:

```text
nitel-estate
│
├── resident
├── property
├── vehicle
├── billing
├── payment
├── visitor
├── complaint
├── notification
├── announcement
├── user
├── audit
└── reporting
```

Each module can have its own:

```text
Controller
Service
Repository
Entity
DTO
```

but all modules initially run in one deployable application.

This makes the system easier to develop, test, deploy and operate.

---

# Database

Use **PostgreSQL**.

A simplified relationship looks like:

```text
                    RESIDENT
                       │
             ┌─────────┼─────────┐
             │         │         │
             ▼         ▼         ▼
          PROPERTY   VEHICLE   ACCOUNT
                       │         │
                       │         ▼
                       │      INVOICE
                       │         │
                       │         ▼
                       │       PAYMENT
                       │
                       ▼
                 ACCESS_EVENT
                       │
                       ▼
                    ANPR
```

The key architectural principle is that ANPR should not maintain its own resident database.

When the camera detects a plate:

```text
Camera detects:

LAG123AB

       ↓

Vehicle table

       ↓

Resident

       ↓

Account

       ↓

Access Policy

       ↓

ALLOW / DENY
```

---

# API-First Architecture

Even though the first application may primarily be a web application, expose clean APIs.

Example endpoints:

```http
POST   /api/v1/residents
GET    /api/v1/residents/{id}
PUT    /api/v1/residents/{id}

POST   /api/v1/properties
GET    /api/v1/properties/{id}

POST   /api/v1/vehicles
GET    /api/v1/vehicles/{plate}

GET    /api/v1/accounts/{residentId}
GET    /api/v1/accounts/{residentId}/balance

POST   /api/v1/payments
GET    /api/v1/payments/{id}

POST   /api/v1/visitors
GET    /api/v1/visitors/{id}

POST   /api/v1/complaints
GET    /api/v1/complaints/{id}
```

When ANPR is introduced, add an access decision endpoint:

```http
POST /api/v1/access/check
```

Example request:

```json
{
  "plateNumber": "LAG123AB",
  "gateId": "MAIN-GATE-01"
}
```

Example response:

```json
{
  "allowed": true,
  "reason": "ACCOUNT_IN_GOOD_STANDING"
}
```

This API becomes the bridge between the estate-management platform and the physical gate infrastructure.

---

# Implementation Roadmap

## Phase 1 — Nitel Estate MVP

Focus on:

```text
Residents
Properties
Vehicles
Dues
Invoices
Payments
Admin
```

## Phase 2 — Resident Experience

Add:

```text
Resident Portal
Visitor Management
Complaints
Announcements
Notifications
Online Payments
Reports
```

## Phase 3 — Security

Add:

```text
Security Dashboard
Gate Management
Access Policies
Security Officer App
Access Logs
RFID
```

## Phase 4 — Smart Gate

Integrate:

```text
                 ANPR CAMERA
                      │
                      ▼
                Plate Number
                      │
                      ▼
                NEMS API
                      │
                Access Policy
                      │
                ┌─────┴─────┐
                │           │
              ALLOW        DENY
                │           │
                ▼           ▼
            OPEN GATE    KEEP CLOSED
```

---

# Long-Term Opportunity

If the platform is designed correctly, Nitel Estate can become the first deployment rather than the final product.

The platform could eventually become a multi-tenant SaaS product:

```text
                    NEMS PLATFORM
                         │
          ┌──────────────┼──────────────┐
          │              │              │
       Estate A       Estate B       Estate C
          │              │              │
     Residents       Residents       Residents
     Properties      Properties      Properties
     Billing         Billing         Billing
     Gates           Gates           Gates
```

Each estate would have complete tenant isolation while sharing the same underlying platform.

This creates the possibility of eventually offering **NEMS — Nigerian Estate Management System** to other residential estates.

---

# Recommended Strategy

The recommended approach is:

1. Build the estate-management foundation first.
2. Establish a reliable resident/property registry.
3. Build vehicle registration before ANPR.
4. Build billing and payment management.
5. Add visitor management.
6. Introduce role-based administration.
7. Add audit logs and reporting.
8. Introduce access policies.
9. Integrate ANPR/RFID and gate controllers.
10. Eventually evolve the platform into a multi-estate SaaS solution.

The key architectural principle is:

> **Build the software foundation first and treat ANPR/gate access as an integration layer.**

This reduces initial cost and complexity while ensuring that the work done for the first phase directly supports the eventual smart-gate system.
