-- Adds per-user read/unread tracking for announcements, backing the frontend notification bell.

create table announcement_read (
    id                  bigserial primary key,
    created_at          timestamp not null,
    updated_at          timestamp not null,
    announcement_id     bigint not null references announcement(id),
    user_id             bigint not null,
    read_at             timestamp not null,
    unique (announcement_id, user_id)
);

create index idx_announcement_read_user_id on announcement_read(user_id);
