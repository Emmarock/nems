alter table resident add column claim_otp_hash varchar(64);
alter table resident add column claim_otp_expires_at timestamp;
alter table resident add column claim_otp_attempts integer not null default 0;

create index idx_resident_claim_otp_hash on resident(claim_otp_hash);
