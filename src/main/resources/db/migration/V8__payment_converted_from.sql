-- REQ-065.AC10: a partial conversion creates a new REGULAR payment that points at the ADDITIONAL payment it came from.
ALTER TABLE payments ADD COLUMN converted_from bigint REFERENCES payments (id);
