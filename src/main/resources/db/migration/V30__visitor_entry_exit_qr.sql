ALTER TABLE visitor RENAME COLUMN qr_token TO qr_token_entry;
ALTER TABLE visitor ADD COLUMN qr_token_exit VARCHAR(255);
UPDATE visitor SET qr_token_exit = gen_random_uuid()::text;
ALTER TABLE visitor ALTER COLUMN qr_token_exit SET NOT NULL;
ALTER TABLE visitor ADD CONSTRAINT uk_visitor_qr_token_exit UNIQUE (qr_token_exit);
