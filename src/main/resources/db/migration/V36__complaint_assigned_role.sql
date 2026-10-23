-- Repurpose the free-text "assigned to" field as a role to route a complaint to (security,
-- finance, treasurer, etc.) instead of an unstructured name nothing in the app ever actually set.
alter table complaint rename column assigned_to to assigned_role;
alter table complaint alter column assigned_role type varchar(24);
