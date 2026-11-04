-- Which AWS Rekognition face ids belong to which registered person (see facematch.FaceIndexService),
-- so a re-uploaded or removed photo can replace/delete the old face, and a match can be traced back.
create table face_index_entry (
    id           bigserial primary key,
    subject_type varchar(32)  not null,
    subject_id   bigint       not null,
    face_id      varchar(64)  not null unique,
    created_at   timestamptz  not null,
    updated_at   timestamptz  not null
);
create index idx_face_index_subject on face_index_entry (subject_type, subject_id);
