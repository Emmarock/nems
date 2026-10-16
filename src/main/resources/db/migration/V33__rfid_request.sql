alter table rfid_tag add column assigned_visitor_id bigint;

create table rfid_request (
    id                          bigserial primary key,
    created_at                  timestamp not null,
    updated_at                  timestamp not null,
    requested_by_resident_id    bigint not null,
    subject_type                varchar(16) not null,
    subject_id                  bigint not null,
    status                      varchar(16) not null,
    issued_tag_id               bigint
);

create index idx_rfid_request_status on rfid_request(status);
create index idx_rfid_request_requested_by_resident_id on rfid_request(requested_by_resident_id);
