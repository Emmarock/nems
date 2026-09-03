-- Backs the forced first-login password reset: a bulk-created or admin-reset account is
-- usable to log in, but every other endpoint is denied (see CustomUserDetails) until the
-- holder sets their own password via PUT /api/v1/auth/password.
alter table app_user add column must_change_password boolean not null default false;
