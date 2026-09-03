-- Gives each property ("building") its own QR pass, scanned during enforcement walkthroughs to
-- pull up the owning resident's payment history/outstanding balance on the spot — distinct from
-- the resident/visitor/worker passes, which are about gate entry, not payment status.
-- Generated lazily on first request (see PropertyService.getOrCreateQrToken) rather than
-- backfilled here, so existing properties pick one up the first time it's needed.

alter table property add column qr_token varchar(64);
alter table property add constraint uq_property_qr_token unique (qr_token);

create index idx_property_qr_token on property(qr_token);
