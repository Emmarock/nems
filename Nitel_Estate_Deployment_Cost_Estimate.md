# Nitel Estate — Deployment Cost Estimate (1 Year)

· Presented By Chief Babajide Apata - Sep 20, 2026 (updated Nov 4, 2026)

## Bottom line

Running Nitel Estate's management platform (hosting + resident SMS and email notifications + CCTV face search) for **1 year** is estimated at **₦6,887,000**. 
This excludes payment gateway fees and RFID hardware (see below). Also the initial domain registration is for a period of 5 years.

CCTV face search is **optional** and budgeted at **₦625,000 per year** (AWS Rekognition, including indexing of every registered photo). Without it, the 1-year estimate is **₦6,262,000**. Email notifications are budgeted at **₦1,728,000 per year**.

## Cost breakdown (1 year)

| Item                                        | Cost                   | Type                          |
|---------------------------------------------|------------------------|-------------------------------|
| Hosting (Amazon web service + PostgreSQL, sized for 3,600 residents) | ₦3,283,000 | Recurring (annual)            |
| SMS notifications (3,600 residents)          | ₦605,000              | Recurring (annual)            |
| Custom domain (https://www.nitelestate.com) | ₦296,000               | Recurring (every 5 Years) *** |
| Email notifications (Resend, ~3,600 accounts) | ₦1,728,000 | Recurring (annual) |
| CCTV face search (AWS Rekognition, ~21,000 faces) | ₦625,000 | Recurring (optional) |
| Smartphones (2x, gate scanning — mid-range) | ₦350,000               | One-off                   *** |
| RFID hardware (Phase 3, one-time)           | Pending vendor quote   | One-off                   *** |
| **Total (with CCTV face search)**           | **₦6,887,000**         |                               |
| **Total (without CCTV face search)**        | **₦6,262,000**         |                               |

## Assumptions

- **Hosting:** sized for 3,600 residents (600 landlords + 3,000 tenants), gate scanning at peak times, and ~21,000 stored photos. About $171/mo ≈ $2,052/year ≈ ₦3,283,000. USD converted at ₦1,600/$1.
  - *Application servers (~$100/mo):* 2 × AWS EC2 t3.medium (2 vCPU, 4 GB each, ~$61/mo together) behind an Application Load Balancer (~$22/mo), so the gate keeps working if one server fails or during updates. Plus ~$9/mo data transfer and ~$8/mo monitoring and logs.
  - *Database (~$71/mo):* AWS RDS PostgreSQL db.t3.medium (2 vCPU, 4 GB, ~$60/mo) with 50 GB storage (~$6/mo) and daily automated backups (~$5/mo). 50 GB covers the ~6.3 GB of resident and worker photos with plenty of room to grow.
  - Running the database on two servers (Multi-AZ, automatic failover) would add about $60/mo (≈ ₦1,150,000/year). It is not included here.
  - Prices are for AWS's US East region. Hosting in Cape Town (af-south-1), the region closest to Nigeria, typically costs 15–20% more.
- **Notifications:** \~2 SMS messages per resident per month (visitor arrival alerts, payment confirmations to landlords, worker alerts, account messages, and occasional urgent announcements), local SMS at \~₦7/message. WhatsApp is not used, so there are no WhatsApp charges. Routine announcements, levy reminders and receipts go by email and the portal instead. 3,600 residents (600 landlords + 3,000 tenants) × 2 messages × 12 months × ₦7 ≈ ₦604,800, rounded to ₦605,000.
- **Email notifications:** sent through Resend (the email provider the platform already uses) from noreply@nitelestate.com. Covers account invites, password resets, levy and arrears reminders, payment confirmations with PDF receipts, checkout reminders and estate announcements. About 25 emails per landlord or tenant per month: 3,600 accounts × 25 ≈ 90,000 emails a month. That fits Resend's Scale plan (100,000 emails/month) at $90/mo = $1,080/year ≈ ₦1,728,000. Resend charges per plan, not per email, so this covers up to ~27 emails per account per month; beyond that the next tier (200,000 emails) is about $160/mo ≈ ₦3,072,000/year.
- **CCTV face search (AWS Rekognition, optional):** the platform runs fully without it. It can be switched on or off at any time with a server setting, and nothing is charged by AWS while it is off. Budgeted at $0.0075 per Rekognition call (each photo indexed or searched) and $0.00001 per stored face per month. USD converted at ₦1,600/$1.
  - *Estate size:* 600 landlords, each with ~5 tenants (3,000 tenants). Household members: 4 per tenant (12,000) + 5 per landlord (3,000) = 15,000. Workers: up to 10 per landlord (6,000), budgeted at the top of the range because landlords still developing their properties register many artisans and labourers. Total ≈ 21,000 faces.
  - *Annual Rekognition:* (a) storing 21,000 faces: 21,000 × $0.00001 × 12 = $2.52; (b) ~6,750 new or changed photos a year: 25% household turnover as tenants move in and out (3,750) + 50% worker turnover as construction crews change (3,000): 6,750 × $0.0075 = $50.63; (c) up to 500 CCTV searches a month, each about 4 Rekognition calls (one to find the faces in the still, one per face to search): 500 × 4 × 12 × $0.0075 = $180.00. Subtotal ≈ $233.15 ≈ ₦373,000.
  - *Infrastructure:* no extra server is needed; face search runs inside the existing hosting. The photos are stored in the existing database, whose storage is already counted in the hosting line.
  - *Indexing:* every registered photo is indexed when the feature is switched on: 21,000 × $0.0075 = $157.50 ≈ ₦252,000. This is included in the yearly figure so the line stays the same every year; after the first year it acts as headroom (e.g. for re-indexing everyone if the face index ever has to be rebuilt).
  - *Yearly total:* ₦373,000 + ₦252,000 ≈ ₦625,000.
  - Landlords and tenants themselves are not indexed. Only household members and workers are, as the feature is built today.
  - New AWS accounts may get some of this free in the first year; that saving is not counted here.
- **Buffer:** all figures above are the raw estimate, to absorb exchange-rate swings, provider price increases Prices change; verify current rates before committing budget.
- **Smartphones:** 2x mid-range Android phones for gate scanning, \~₦350,000 total. A budget-Android option is available for less if the committee prefers to cut this line.

## Not included

- **Payment gateway fees** — the platform currently uses manual receipt upload + treasurer approval, not a live payment gateway. If the committee later wants direct online dues collection (e.g. Paystack/Flutterwave, \~1.5% per transaction), this can be sized once we have typical annual levy volume.
- **CCTV cameras and recording equipment** — the face search works on stills taken from the estate's existing CCTV footage. Buying or upgrading cameras is not included.
- **RFID hardware** (Phase 3 physical security) — gate readers, tags, and installation are a one-time procurement cost, not a recurring cloud expense. Added to the cost table above as "pending vendor quote" — will fill in real figures once the quote comes in.
