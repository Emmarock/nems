-- Multi-party control over the account residents pay into: a proposed change only takes effect
-- once two OTHER eligible roles (distinct from the proposer's own role) approve it - see
-- PaymentAccountService. One row per proposal, one row per role's decision on it.
create table payment_account_change_request (
    id                   bigserial primary key,
    created_at           timestamp not null,
    updated_at           timestamp not null,
    bank_name            varchar(255) not null,
    account_number       varchar(32) not null,
    account_name         varchar(255) not null,
    proposed_by_user_id  bigint not null,
    proposed_by_role     varchar(32) not null,
    status               varchar(16) not null,
    decided_at           timestamp
);

create table payment_account_change_approval (
    id                 bigserial primary key,
    created_at         timestamp not null,
    updated_at         timestamp not null,
    change_request_id  bigint not null references payment_account_change_request(id),
    user_id            bigint not null,
    role               varchar(32) not null,
    decision           varchar(16) not null,
    notes              varchar(500)
);

create index idx_payment_account_change_approval_request on payment_account_change_approval(change_request_id);
