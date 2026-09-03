-- Gives residents their own gate-access QR pass (previously only visitors/workers had one),
-- so security can scan a resident directly at the gate, not just their guests/contractors.
-- Generated lazily on first request (see ResidentService.getOrCreateQrToken) rather than
-- backfilled here, so existing residents pick one up the first time it's needed.

alter table resident add column qr_token varchar(64);
alter table resident add constraint uq_resident_qr_token unique (qr_token);

create index idx_resident_qr_token on resident(qr_token);
