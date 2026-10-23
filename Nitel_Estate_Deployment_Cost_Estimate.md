# Nitel Estate — Deployment Cost Estimate (1 Year)

· Presented By Chief Babajide Apata - Sep 20, 2026

## Bottom line

Running Nitel Estate's management platform (hosting + resident SMS/WhatsApp notifications) for **1 year** is estimated at **₦4,078,000**. 
This excludes payment gateway fees and RFID hardware (see below). Also the initial domain registration is for a period of 5 years

## Cost breakdown (1 year)

| Item                                        | Cost                   | Type                          |
|---------------------------------------------|------------------------|-------------------------------|
| Hosting (Amazon web service + PostgreSQL)   | ₦2,592,000             | Recurring (annual)            |
| SMS/WhatsApp notifications (500 residents)  | ₦840,000               | Recurring (optional)          |
| Custom domain (https://www.nitelestate.com) | ₦296,000               | Recurring (every 5 Years) *** |
| Smartphones (2x, gate scanning — mid-range) | ₦350,000               | One-off                   *** |
| RFID hardware (Phase 3, one-time)           | Pending vendor quote   | One-off                   *** |
| **Total**                                   | **₦4,078,000**         |                               |

## Assumptions

- **Hosting:** Amazon Web Service Starter package ($70/mo) plus a paid, managed Amazon Web Service Starter Database Postgres tier ($65/mo). USD converted at ₦1,600/$1.
- **Notifications:** \~20 SMS/WhatsApp messages per resident per month (levy reminders, payment confirmations, estate announcements, message to visitor e.t.c), mostly local SMS at \~₦7/message.
- **Buffer:** all figures above are the raw estimate, to absorb exchange-rate swings, provider price increases Prices change; verify current rates before committing budget.
- **Smartphones:** 2x mid-range Android phones for gate scanning, \~₦350,000 total. A budget-Android option is available for less if the committee prefers to cut this line.

## Not included

- **Payment gateway fees** — the platform currently uses manual receipt upload + treasurer approval, not a live payment gateway. If the committee later wants direct online dues collection (e.g. Paystack/Flutterwave, \~1.5% per transaction), this can be sized once we have typical annual levy volume.
- **RFID hardware** (Phase 3 physical security) — gate readers, tags, and installation are a one-time procurement cost, not a recurring cloud expense. Added to the cost table above as "pending vendor quote" — will fill in real figures once the quote comes in.
