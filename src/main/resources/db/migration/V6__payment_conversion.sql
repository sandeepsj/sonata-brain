-- REQ-065.AC8: audit of additional payments converted into course fees. Null for payments never converted.
ALTER TABLE payments ADD COLUMN original_type varchar(16) CHECK (original_type IN ('REGULAR', 'ADDITIONAL'));
ALTER TABLE payments ADD COLUMN converted_at timestamp;
ALTER TABLE payments ADD COLUMN converted_by varchar(255);
