-- app_user.resident_id has been a bare bigint since V1 - no referential integrity at the DB
-- level, and no column tying it to resident.full_name once it's copied in. That let the two
-- diverge silently: app_user.full_name is only ever copied from resident.full_name once, at
-- account-creation/bulk-provisioning/claim time (see UserService.create/bulkCreateResidentAccounts/
-- verifyClaimAccount) - a later edit to the resident's name (ResidentService.update/updateSelf)
-- never propagated back, so the header/login name silently fell out of step with the resident's
-- actual current name. The application-level fix (keeping them in sync going forward) is in
-- ResidentService; this migration fixes the two things only a migration can: the already-diverged
-- rows, and the missing constraint.

-- One-time backfill: pull every linked app_user's name back in line with its resident's current
-- name. Safe to run unconditionally - a no-op for every row that's already in sync.
update app_user
set full_name = resident.full_name
from resident
where app_user.resident_id = resident.id
  and app_user.full_name <> resident.full_name;

-- Links app_user.resident_id to a real resident row. Nullable stays nullable (SUPER_ADMIN,
-- SECURITY, and every other non-resident-role account legitimately has no resident_id at all) -
-- this only constrains the column when it IS set. No ON DELETE clause: deleting a resident while
-- a login account still points at it should fail loudly, not silently orphan or null out the
-- link - whoever deletes a resident is expected to handle its app_user row explicitly, same as
-- the convention already used when cleaning up duplicate/test accounts.
alter table app_user
    add constraint fk_app_user_resident foreign key (resident_id) references resident (id);
