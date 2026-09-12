-- Where residents should send money for any levy - a single admin-configurable account,
-- since there's no online gateway integration yet (see V16/V17/V18): residents pay by bank
-- transfer and then submit a receipt. Seeded blank; an admin fills it in before residents can
-- see real details.
create table payment_account_settings (
    id              bigserial primary key,
    created_at      timestamp not null,
    updated_at      timestamp not null,
    bank_name       varchar(255) not null default '',
    account_number  varchar(32) not null default '',
    account_name    varchar(255) not null default ''
);

insert into payment_account_settings (created_at, updated_at, bank_name, account_number, account_name)
values (now(), now(), '', '', '');
