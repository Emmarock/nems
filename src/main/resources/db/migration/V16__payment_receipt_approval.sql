-- Lets a resident submit their own payment receipt (a photo of the bank alert/transfer slip)
-- for a treasurer or financial secretary to review, instead of only staff being able to record a
-- payment directly. Widening status to fit PENDING_APPROVAL (17 chars, was varchar(16)).
alter table payment alter column status type varchar(20);

alter table payment add column receipt_image text;
alter table payment add column approved_by_user_id bigint;
alter table payment add column approved_at timestamp;
alter table payment add column review_notes varchar(500);
