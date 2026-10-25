-- Generalizes the announcement "photo" into a single attachment slot that also accepts a PDF
-- (e.g. the secretary sharing meeting minutes) - still mutually exclusive with SMS either way,
-- enforced in AnnouncementService.create.
alter table announcement rename column photo to attachment;
alter table announcement add column attachment_filename varchar(255);
