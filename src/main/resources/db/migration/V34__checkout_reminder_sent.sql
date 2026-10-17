-- Keeps the "pass/contract is overdue" reminder a one-time alert instead of repeating every
-- 5 minutes through the evening window for as long as the pass stays unresolved.
alter table visitor add column checkout_reminder_sent boolean not null default false;
alter table worker add column checkout_reminder_sent boolean not null default false;
