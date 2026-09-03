-- Gives each registered vehicle its own QR pass, viewable by the resident who registered it
-- (see VehicleService.getOrCreateQrTokenForResident) — distinct from the resident's own gate
-- pass, since a resident may register several vehicles.
-- Generated lazily on first request rather than backfilled here, so existing vehicles pick one
-- up the first time it's needed.

alter table vehicle add column qr_token varchar(64);
alter table vehicle add constraint uq_vehicle_qr_token unique (qr_token);

create index idx_vehicle_qr_token on vehicle(qr_token);
