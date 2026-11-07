-- Which resident currently holds each estate office (Secretary, Treasurer, PRO, ...), assigned by
-- the super admin. Office notifications fall back to this resident's email when the office account
-- has no personal email set (OfficeNotificationService). One holder per office.
create table office_holder (
    id                  bigserial primary key,
    role                varchar(32) not null unique,
    resident_id         bigint      not null references resident (id),
    assigned_by_user_id bigint,
    created_at          timestamptz not null,
    updated_at          timestamptz not null
);
