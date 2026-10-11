-- Existing rows backfilled to NON_DOMESTIC_STAFF (the contractor/artisan case the pre-existing
-- test data already matches, e.g. "Plumbing"/"Building Contractor"/"Wiring") - new requests
-- always supply a real choice via WorkerRequest's @NotNull validation.
alter table worker add column worker_category varchar(24) not null default 'NON_DOMESTIC_STAFF';
