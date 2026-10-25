-- Optional image attached to an announcement (shown in the portal and emailed as an attachment;
-- not deliverable via SMS, enforced in AnnouncementService.create).
alter table announcement add column photo text;
