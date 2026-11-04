-- Marks that the one-time backfill of existing worker/household photos into the face index has
-- run (see facematch.FaceIndexBackfillJob), so it never runs again on later restarts.
create table face_index_backfill (
    id           bigserial primary key,
    indexed      int         not null,
    total        int         not null,
    completed_at timestamptz not null
);
