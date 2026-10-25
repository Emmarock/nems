-- Lets the creator exclude tenants from an announcement's audience (e.g. an owners-only notice) -
-- defaults true so every existing announcement keeps reaching everyone, same as before.
alter table announcement add column include_tenants boolean not null default true;
