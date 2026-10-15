-- Exit QR is now generated lazily, on the visitor's first check-in, instead of upfront —
-- existing rows keep whatever exit token they already have.
ALTER TABLE visitor ALTER COLUMN qr_token_exit DROP NOT NULL;
