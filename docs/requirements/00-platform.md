# Platform and process

### REQ-001 Backend and API only, no backward compatibility
Status: accepted · Source: user, 2026-10-04

The work covers the backend and its HTTP API only. Existing endpoints may change shape. The frontend will be updated
to the new contract later.

- **REQ-001.AC1** [process] Every endpoint in the new contract is documented by at least one requirement in this log.

### REQ-002 Tests before code, written as a language-neutral contract
Status: accepted · Source: user, 2026-10-04 (YAML format chosen the same day)

An exhaustive black-box test suite is written before feature code, and every code change is checked against it. The
suite is the API contract. It lives in `contract/` as YAML, so the backend could be rewritten in any language and
checked against the same files.

- **REQ-002.AC1** [process] Every test case is fully described in YAML: requests, data and expected results. The
  runner (`contract/runner/`) holds no case-specific logic or assertions. Its vocabulary is documented in
  `contract/README.md`.
- **REQ-002.AC2** [process] `python3 contract/run.py` runs the `baseline` suite and must be green on every commit.
  `--suite target` reports which parts of the new contract are still red.
- **REQ-002.AC3** [process] The runner starts the backend against a freshly created database, and empties all
  business tables before each case. Cases don't depend on each other or on their order.

### REQ-003 Requirements log with history
Status: accepted · Source: user, 2026-10-04

- **REQ-003.AC1** [process] Every acceptance criterion in `docs/requirements/*.md` is covered by at least one contract
  case (`req:`), and every `req:` refers to an existing criterion. The runner reports both and fails on unknown
  citations. `--strict-trace` also fails on uncovered criteria.
- **REQ-003.AC2** [process] `HISTORY.md` has a dated entry for every requirement that is added, changed or dropped.

### REQ-004 Production data is preserved and rehearsed locally
Status: accepted · Source: user, 2026-10-04

- **REQ-004.AC1** [process] `scripts/db/dump-prod.sh` takes a read-only dump of the Neon production database over the
  direct (non-pooled) connection into `~/sonata-dumps/`. Dumps are never committed.
- **REQ-004.AC2** [process] `scripts/db/reset-local-from-prod.sh` recreates `sonata-local` as an exact copy of the
  newest dump, and refuses to run against a non-local host.

### REQ-005 One error format
Status: proposed · Source: design, 2026-10-04

Today there are two competing exception handlers and several ad-hoc error shapes, and some errors have empty bodies.

- **REQ-005.AC1** [target] Every 4xx/5xx response has a JSON body
  `{status, error, message, details, timestamp}`. `details` is a list of `{field, message}` and is empty when there
  is nothing field-specific.
- **REQ-005.AC2** [target] Bean-validation failures return 400 with one `details` entry per invalid field.
- **REQ-005.AC3** [target] An unknown resource ID returns 404. A business-rule conflict (duplicate, already
  deactivated, overlap) returns 409. A malformed request returns 400. Internal details (stack traces, SQL) are never
  returned.

### REQ-006 Versioned schema migrations
Status: accepted · Source: design, 2026-10-04

- **REQ-006.AC1** [target] The schema is created only by Flyway migrations. Hibernate runs with `ddl-auto=validate`.
- **REQ-006.AC2** [target] The baseline migration reproduces the production schema of 2026-10-04 exactly. Later
  migrations apply cleanly both on an empty database and on the production copy.

### REQ-007 Health check
Status: accepted · Source: existing code, 2026-10-04

- **REQ-007.AC1** [baseline] `GET /heartbeat` without a token → 200, body `OK`.
