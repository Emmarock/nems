-- Pre-generated QR tags: printed in bulk as sticker sheets (QrTagSheetPdf), then linked to a vehicle
-- or property by their printed code (QrTagService.assign). Linking copies the tag's token onto the
-- vehicle/property's qr_token, so gate scans and lookups work unchanged.
--
-- A tag is AVAILABLE while subject_id is null. Once linked it can never be linked again: if the
-- subject later gets a different token (re-linked, or reactivated), the old sticker is dead (VOID)
-- rather than reusable, so a sticker already on one car can never start pointing at another.
create table qr_tag (
    id                  bigserial primary key,
    kind                varchar(16) not null,
    serial              integer     not null,
    code                varchar(16) not null unique,
    token               varchar(64) not null unique,
    batch_no            integer     not null,
    subject_id          bigint,
    assigned_at         timestamptz,
    assigned_by_user_id bigint,
    created_at          timestamptz not null,
    updated_at          timestamptz not null,
    unique (kind, serial)
);

create index idx_qr_tag_batch on qr_tag (batch_no);
create index idx_qr_tag_subject on qr_tag (kind, subject_id);
