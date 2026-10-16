-- REQ-041, REQ-042: courses can be closed and reopened. Existing courses are active.
ALTER TABLE course ADD COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED'));
ALTER TABLE course ADD COLUMN closed_at timestamp;
