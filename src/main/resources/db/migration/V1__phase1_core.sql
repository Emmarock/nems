-- Phase 1 — Estate Management MVP
-- Residents, Properties, Vehicles, Dues/Billing, back-office Payments, Admin (users), Audit.

create table app_user (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    email           varchar(255) not null unique,
    password_hash   varchar(255) not null,
    full_name       varchar(255) not null,
    role            varchar(32) not null,
    status          varchar(16) not null,
    resident_id     bigint
);

create table resident (
    id                  bigserial primary key,
    created_at          timestamp not null,
    updated_at          timestamp not null,
    full_name           varchar(255) not null,
    phone               varchar(32) not null,
    email               varchar(255),
    property_id         bigint,
    resident_type       varchar(16) not null,
    emergency_contact   varchar(255),
    status              varchar(16) not null,
    registration_date   date not null
);

create table property (
    id                  bigserial primary key,
    created_at          timestamp not null,
    updated_at          timestamp not null,
    block               varchar(64) not null,
    plot                varchar(64) not null,
    house_number        varchar(64) not null unique,
    address             varchar(500) not null,
    property_type       varchar(32) not null,
    owner_id            bigint,
    occupancy_status    varchar(24) not null
);

create table vehicle (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    plate_number    varchar(32) not null unique,
    vehicle_type    varchar(64),
    make            varchar(64),
    model           varchar(64),
    colour          varchar(32),
    resident_id     bigint not null,
    status          varchar(16) not null
);

create table levy (
    id          bigserial primary key,
    created_at  timestamp not null,
    updated_at  timestamp not null,
    name        varchar(255) not null,
    amount      numeric(14,2) not null,
    frequency   varchar(16) not null,
    active      boolean not null default true
);

create table invoice (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    resident_id     bigint not null,
    levy_id         bigint not null,
    description     varchar(255) not null,
    amount          numeric(14,2) not null,
    issue_date      date not null,
    due_date        date not null,
    status          varchar(16) not null
);

create table payment (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    resident_id             bigint not null,
    invoice_id              bigint,
    amount                  numeric(14,2) not null,
    method                  varchar(24) not null,
    provider                varchar(32),
    provider_reference      varchar(128),
    status                  varchar(16) not null,
    paid_at                 timestamp not null,
    recorded_by_user_id     bigint
);

create table audit_log (
    id              bigserial primary key,
    occurred_at     timestamp not null,
    entity_type     varchar(64) not null,
    entity_id       varchar(64) not null,
    action          varchar(64) not null,
    actor           varchar(255) not null,
    details         varchar(1000)
);

create index idx_resident_property_id on resident(property_id);
create index idx_vehicle_resident_id on vehicle(resident_id);
create index idx_invoice_resident_id on invoice(resident_id);
create index idx_payment_resident_id on payment(resident_id);
create index idx_payment_provider_reference on payment(provider_reference);
create index idx_audit_log_entity on audit_log(entity_type, entity_id);
