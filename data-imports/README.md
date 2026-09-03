# Nitel Estate Residents 2026 — one-time data import

Loads `Residents 2026 Outstanding.xlsx` into residents, properties, levies, invoices and
payments. This **replaced** an earlier 2025 dataset entirely (not a merge) — the files from that
import have been deleted now that they're no longer used.

**This is deliberately not a Flyway migration** (it doesn't live in `src/main/resources/db/migration`
and isn't prefixed `V#__`) — it's real historical data for one specific estate, not schema or
reference data every environment should get automatically (including ephemeral test databases).
Run it once, deliberately, against whichever database should receive this data.

## Files

- `generate_2026_residents_import.py` — parses the source `.xlsx` (only 4 of its ~55 sheets are
  used — see the script's docstring for exactly which, and why) and regenerates
  `2026_residents_import.sql` + `2026_import_report.md`. Requires `pip install openpyxl`.
- `2026_truncate.sql` — **run this first.** Clears the previous dataset (residents/properties/
  levies/invoices/payments/vehicles — workers, visitors, access logs, complaints, announcements,
  and login accounts are untouched). Deliberately scoped, not a blanket wipe — see the comment in
  the file for exactly what it does and doesn't touch.
- `2026_residents_import.sql` — the data load, a single PL/pgSQL `DO` block. Guards itself
  against double-application (aborts if `ROAD A-001` already exists).
- `2026_import_report.md` — **read this before running.** Documents every duplicate/unmatched/
  flagged row, every consolidated multi-property resident, and every assumption made.

## Running it

```bash
docker exec -i nitel-estate-postgres psql -U nitel -d nitel_estate < 2026_truncate.sql
docker exec -i nitel-estate-postgres psql -U nitel -d nitel_estate < 2026_residents_import.sql
```

## What it creates

- 7 `Levy` rows: **Security Fee + Yearly Devt. Levy** (merged into one — their only source of
  truth, the "NEW 2026 DEBTORS" sheet, reports them as a single combined figure per resident),
  **Devt. Entrance Levy**, **Membership Form**, **Electricity/Transformer Levy**,
  **2nd Transformer Payment**, **Donation**, and a synthetic **2024/2025 Arrears** for
  carried-over debt.
- One `Property` + `Resident` (type `OWNER`) per *unique* roster name (432 residents) — exact
  duplicate roster rows are skipped (first occurrence wins), per instruction. A resident whose
  name recurs under a *different* road on a levy sheet (a real multi-property owner) still gets
  only one resident record; that sheet's amounts for them are summed rather than dropped.
- Invoices+payments for every levy amount present on the relevant sheet (a value there means
  *paid*, per every sheet's own "2026 Payment" title). Unpaid 24/2025 arrears get an invoice with
  no matching payment, so they correctly show up as still outstanding.

## Known limitations (see the report for details)

- 43 residents present on "NEW 2026 DEBTORS" don't appear on the roster sheet at all, so have
  nowhere to attach a resident record to — their amounts were skipped, not guessed at.
- 20 cells were free text ("waived", "old member", "to balance 140k at month end july", …) or
  implausibly large numbers that were almost certainly a phone number typed into the wrong column
  (one **16.4 billion naira** "donation" cell) — all skipped, not silently included.
- Real plot/house numbers are almost entirely absent from this workbook, so house numbers are
  synthesized as `{ROAD}-{seq}` for every resident.
