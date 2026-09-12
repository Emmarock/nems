-- Correction: there isn't one shared account for every levy - the estate actually operates
-- separate accounts per purpose (e.g. Electricity, Development), so payment_account_settings'
-- singleton-row design is wrong. Turning it into a proper multi-row, labelled table instead of a
-- brand new one, so the one row it already had (and its change-approval history) carries forward
-- rather than being orphaned.
alter table payment_account_settings rename to payment_account;
alter table payment_account add column label varchar(100) not null default '';
alter table payment_account add column active boolean not null default true;

-- The existing singleton row becomes the first of the two known real accounts; the second is
-- added alongside it. Both start blank - filled in via the approval workflow, same as before.
update payment_account set label = 'Electricity' where label = '';
insert into payment_account (created_at, updated_at, label, bank_name, account_number, account_name, active)
values (now(), now(), 'Development', '', '', '', true);

-- Which account a levy's payments actually go to - nullable, since not every levy necessarily
-- has one assigned yet (the admin links each levy to the right account after this migration).
alter table levy add column payment_account_id bigint references payment_account(id);

-- A change request now targets one specific account (null = proposing a brand new account
-- rather than editing an existing one), and carries the proposed label alongside the proposed
-- bank details.
alter table payment_account_change_request add column target_account_id bigint references payment_account(id);
alter table payment_account_change_request add column label varchar(100) not null default '';

-- Backfill: the one change-approval history that exists so far was implicitly against the
-- "Electricity" row (the former singleton).
update payment_account_change_request
set target_account_id = (select id from payment_account where label = 'Electricity'),
    label = 'Electricity';
