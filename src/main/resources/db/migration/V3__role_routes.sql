-- REQ-011/012/016 (2026-10-15): roles grant read and write per route path instead of per resource.

CREATE TABLE role_route (
    role_id   bigint       NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    path      varchar(100) NOT NULL,
    can_read  boolean      NOT NULL,
    can_write boolean      NOT NULL,
    PRIMARY KEY (role_id, path)
);

-- Each old resource covered these paths. READ becomes read; WRITE (which implied READ) becomes read and write.
INSERT INTO role_route (role_id, path, can_read, can_write)
SELECT p.role_id, m.path, true, p.access = 'WRITE'
FROM role_permission p
JOIN (VALUES ('STUDENT', '/api/students/**'),
             ('SESSION', '/api/student-sessions/**'),
             ('PAYMENT', '/api/payments/**'),
             ('PAYMENT', '/api/discounts/**'),
             ('COURSE', '/api/courses/**'),
             ('FEE_STRUCTURE', '/api/fee-structures/**'),
             ('DUES', '/api/dues/**'),
             ('USER', '/api/users/**'),
             ('USER', '/api/roles/**'),
             ('USER', '/api/logging/**')) AS m (resource, path) ON m.resource = p.resource;

DROP TABLE role_permission;
