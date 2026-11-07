-- Staff accounts (secretary, treasurer, PRO, ...) often log in with a shared office address; this
-- is the holder's own email, where task notifications for their office are sent
-- (OfficeNotificationService).
alter table app_user add column personal_email varchar(255);
