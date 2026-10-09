create table household_member (
    id                      bigserial primary key,
    created_at              timestamp not null,
    updated_at              timestamp not null,
    full_name               varchar(255) not null,
    phone                   varchar(32) not null,
    relationship            varchar(16) not null,
    photo                   text,
    property_id             bigint not null,
    sponsor_resident_id     bigint not null,
    status                  varchar(16) not null,
    valid_from              date,
    valid_until             date,
    qr_token                varchar(64) unique
);

create index idx_household_member_sponsor_resident_id on household_member(sponsor_resident_id);
create index idx_household_member_property_id on household_member(property_id);
create index idx_household_member_status on household_member(status);
