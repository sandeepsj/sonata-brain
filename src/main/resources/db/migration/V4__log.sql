-- REQ-008: raw request and application logs, each tagged with the request it belongs to.

CREATE TABLE log (
    id            bigserial    PRIMARY KEY,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    kind          varchar(16)  NOT NULL CHECK (kind IN ('REQUEST', 'APP', 'ERROR')),
    level         varchar(8)   NOT NULL,
    request_id    varchar(64),
    session_id    varchar(128),
    user_email    varchar(255),
    method        varchar(10),
    path          varchar(1024),
    query         varchar(2048),
    status        integer,
    duration_ms   integer,
    logger        varchar(255),
    thread        varchar(100),
    message       text,
    request_body  text,
    response_body text,
    stack_trace   text
);

CREATE INDEX log_request_id_idx ON log (request_id);
CREATE INDEX log_session_id_idx ON log (session_id);
CREATE INDEX log_user_email_idx ON log (user_email);
CREATE INDEX log_created_at_idx ON log (created_at);
