-- One row per vehicle wanting a window sticker. vehicle_id is unique - a sticker is a physical,
-- per-vehicle credential, so a vehicle can never have more than one request (this is also the
-- structural enforcement of "no resident can be issued more stickers than registered vehicles").
create table sticker_request (
    id           bigserial primary key,
    created_at   timestamp not null,
    updated_at   timestamp not null,
    vehicle_id   bigint not null unique,
    resident_id  bigint not null,
    invoice_id   bigint not null,
    status       varchar(20) not null,
    issued_at    timestamp
);
