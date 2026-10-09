alter table app_user add column reset_token_hash varchar(64);
alter table app_user add column reset_token_expires_at timestamp;

create index idx_app_user_reset_token_hash on app_user(reset_token_hash);
