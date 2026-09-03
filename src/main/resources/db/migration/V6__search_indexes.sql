-- Indexes backing the admin search features (global dashboard search + the per-list search
-- boxes on Residents/Properties/Vehicles/Workers/Users).
--
-- The search UI does substring matching (`.includes()` client-side today; the natural
-- server-side equivalent is `ILIKE '%term%'`), which a plain btree index cannot accelerate.
-- Free-text columns get a pg_trgm GIN trigram index instead, which supports fast substring
-- search. Low-cardinality enum-like columns (status/type/role) get plain btree indexes,
-- matching the pattern already used for worker.status and complaint.status in V2.

create extension if not exists pg_trgm;

-- Resident: name/phone/email/type/status are all searched.
create index idx_resident_full_name_trgm on resident using gin (full_name gin_trgm_ops);
create index idx_resident_phone_trgm on resident using gin (phone gin_trgm_ops);
create index idx_resident_email_trgm on resident using gin (email gin_trgm_ops);
create index idx_resident_status on resident(status);
create index idx_resident_resident_type on resident(resident_type);

-- Property: house number/block/plot/address/type/occupancy are all searched.
create index idx_property_house_number_trgm on property using gin (house_number gin_trgm_ops);
create index idx_property_block_trgm on property using gin (block gin_trgm_ops);
create index idx_property_plot_trgm on property using gin (plot gin_trgm_ops);
create index idx_property_address_trgm on property using gin (address gin_trgm_ops);
create index idx_property_type on property(property_type);
create index idx_property_occupancy_status on property(occupancy_status);

-- Vehicle: plate/make/model/colour/type/status are all searched.
create index idx_vehicle_plate_number_trgm on vehicle using gin (plate_number gin_trgm_ops);
create index idx_vehicle_make_trgm on vehicle using gin (make gin_trgm_ops);
create index idx_vehicle_model_trgm on vehicle using gin (model gin_trgm_ops);
create index idx_vehicle_colour_trgm on vehicle using gin (colour gin_trgm_ops);
create index idx_vehicle_type on vehicle(vehicle_type);
create index idx_vehicle_status on vehicle(status);

-- Worker: name/contractor/work type are searched (status already indexed in V2).
create index idx_worker_full_name_trgm on worker using gin (full_name gin_trgm_ops);
create index idx_worker_contractor_name_trgm on worker using gin (contractor_name gin_trgm_ops);
create index idx_worker_work_type_trgm on worker using gin (work_type gin_trgm_ops);

-- app_user: name/email/role/status are all searched (email already has a unique btree index).
create index idx_app_user_full_name_trgm on app_user using gin (full_name gin_trgm_ops);
create index idx_app_user_email_trgm on app_user using gin (email gin_trgm_ops);
create index idx_app_user_role on app_user(role);
create index idx_app_user_status on app_user(status);
