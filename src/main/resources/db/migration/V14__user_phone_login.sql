-- Lets a resident log in with their phone number as an alternative to email - many were bulk-
-- created with a synthetic @nitelestate.local email derived from their phone (see UserService),
-- which isn't something a resident would think to type back in. Nullable + unique: Postgres
-- treats multiple NULLs as non-conflicting, so staff accounts without a phone are unaffected.
alter table app_user add column phone varchar(20);
alter table app_user add constraint uq_app_user_phone unique (phone);

create index idx_app_user_phone on app_user(phone);
