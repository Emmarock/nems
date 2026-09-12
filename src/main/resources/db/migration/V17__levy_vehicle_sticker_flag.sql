-- Marks which levy (if any) is "the" vehicle sticker fee, so the sticker-request flow knows
-- which levy/invoice to charge against without hardcoding a levy id or name.
alter table levy add column vehicle_sticker_levy boolean not null default false;
