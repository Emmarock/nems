-- "2024 Arrears" was never a real levy category — it's the synthetic bucket the 2025 residents
-- import used to carry over each resident's unpaid 2024 balance as an invoice with no matching
-- payment (see data-imports/README.md). It was already marked inactive; now removed from the
-- catalog entirely since it doesn't belong there conceptually.
--
-- Deliberately does NOT touch the 99 real invoices already raised against it (₦8,495,000 of
-- actual resident debt) — there's no FK from invoice to levy, so they're unaffected and keep
-- counting toward outstanding balances exactly as before; only the catalog row is removed.

delete from levy where name = '2024 Arrears';
