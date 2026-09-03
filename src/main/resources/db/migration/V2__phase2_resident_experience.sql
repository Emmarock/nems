-- Phase 2 — Resident Experience
-- Resident Portal, Online Payments, Visitor Management, Worker Module (new),
-- Complaints, Announcements, Notifications, Reporting.

create table access_event (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    subject_type            varchar(16) not null,
    subject_id              bigint not null,
    gate_id                 bigint,
    direction               varchar(8) not null,
    occurred_at             timestamp not null,
    verified_by_user_id     bigint,
    flag_reason             varchar(255)
);

create table visitor (
    id                  bigserial primary key,
    created_at          timestamp not null,
    updated_at          timestamp not null,
    name                varchar(255) not null,
    phone               varchar(32) not null,
    vehicle_plate       varchar(32),
    host_resident_id    bigint not null,
    valid_from          timestamp not null,
    valid_until         timestamp not null,
    qr_token            varchar(64) not null unique,
    status              varchar(16) not null
);

create table worker (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    full_name               varchar(255) not null,
    phone                   varchar(32) not null,
    national_id             varchar(64),
    contractor_name         varchar(255) not null,
    work_type               varchar(255) not null,
    site_id                 bigint,
    sponsor_resident_id     bigint not null,
    start_date              date not null,
    expected_end_date       date not null,
    status                  varchar(16) not null,
    qr_token                varchar(64) unique
);

create table complaint (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    resident_id     bigint not null,
    category        varchar(24) not null,
    description     varchar(2000) not null,
    status          varchar(16) not null,
    priority        varchar(8) not null,
    assigned_to     varchar(255),
    resolved_at     timestamp
);

create table announcement (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    title                   varchar(255) not null,
    message                 varchar(4000) not null,
    created_by_user_id      bigint not null
);

create table announcement_channel (
    announcement_id     bigint not null references announcement(id),
    channel             varchar(16) not null
);

create table notification_log (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    recipient       varchar(255) not null,
    channel         varchar(16) not null,
    message         varchar(2000) not null,
    status          varchar(8) not null
);

create index idx_access_event_subject on access_event(subject_type, subject_id);
create index idx_visitor_host_resident_id on visitor(host_resident_id);
create index idx_worker_sponsor_resident_id on worker(sponsor_resident_id);
create index idx_worker_status on worker(status);
create index idx_complaint_resident_id on complaint(resident_id);
create index idx_complaint_status on complaint(status);
