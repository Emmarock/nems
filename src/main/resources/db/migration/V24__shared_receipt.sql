create table shared_receipt (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    tenant_resident_id      bigint not null,
    landlord_resident_id    bigint not null,
    levy_id                 bigint not null,
    amount                  numeric(14,2) not null,
    method                  varchar(24) not null,
    receipt_image           text,
    status                  varchar(16) not null
);

create index idx_shared_receipt_landlord_resident_id on shared_receipt(landlord_resident_id);
create index idx_shared_receipt_status on shared_receipt(status);
