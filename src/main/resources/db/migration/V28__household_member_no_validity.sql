-- Household member passes no longer carry a validity window — once approved, a pass is valid
-- indefinitely, same as the home owner's own standing, until explicitly revoked.
alter table household_member drop column valid_from;
alter table household_member drop column valid_until;
