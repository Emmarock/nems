-- Marks which levies should be invoiced to a resident automatically the moment they're created
-- (e.g. Membership Form, Devt. Entrance Levy, Annual Security Fee, the vehicle sticker levy) -
-- see InvoiceService.generateDefaultInvoicesForNewResident.
alter table levy add column default_for_new_resident boolean not null default false;
