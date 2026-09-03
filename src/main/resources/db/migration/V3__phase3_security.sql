-- Phase 3 — Security
-- Gate Management, Access Policies, RFID. Security Dashboard/Officer App and Access Logs
-- reuse the access_event table introduced in V2.

create table gate (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    name            varchar(255) not null,
    code            varchar(64) not null unique,
    location        varchar(255),
    type            varchar(16) not null,
    status          varchar(16) not null
);

create table access_policy_settings (
    id                  bigserial primary key,
    created_at          timestamp not null,
    updated_at          timestamp not null,
    enforce_arrears     boolean not null default true,
    arrears_threshold   numeric(14,2) not null default 0
);

create table rfid_tag (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    tag_id                  varchar(64) not null unique,
    assigned_resident_id    bigint,
    assigned_worker_id      bigint,
    vehicle_id              bigint,
    status                  varchar(16) not null
);

insert into access_policy_settings (created_at, updated_at, enforce_arrears, arrears_threshold)
values (now(), now(), true, 0);
