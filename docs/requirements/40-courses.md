# Courses

### REQ-040 Create a course
Status: implemented · Source: user ("allow the user to add new subject"), 2026-10-04

- **REQ-040.AC1** [retired] *(was baseline)* `POST /api/courses` returned 200 `{name, id}`. Replaced by REQ-040.AC3.
- **REQ-040.AC2** [baseline] `GET /api/courses` lists all courses.
- **REQ-040.AC3** [target] `POST /api/courses` `{name}` → 201 `{name, status: "ACTIVE", closedAt: null}`.
- **REQ-040.AC4** [target] A blank name → 400. A name that already exists, ignoring case → 409.
- **REQ-040.AC5** [target] Leading and trailing whitespace is trimmed from the name.

### REQ-041 List and get courses
Status: implemented · Source: user, 2026-10-04

- **REQ-041.AC1** [target] `GET /api/courses` lists all courses. `?status=ACTIVE|CLOSED` filters them. An invalid
  status → 400.
- **REQ-041.AC2** [target] `GET /api/courses/{name}` → 200, or 404 if unknown.

### REQ-042 Close and reopen a course
Status: accepted · Source: user ("mark some subjects as closed"), 2026-10-04

- **REQ-042.AC1** [target] `PATCH /api/courses/{name}/close` → 200 `{status: "CLOSED", closedAt: <now>}`. Closing a
  course that is already closed → 409.
- **REQ-042.AC2** [target] `PATCH /api/courses/{name}/reopen` → 200 `{status: "ACTIVE", closedAt: null}`. Reopening
  a course that is already active → 409.
- **REQ-042.AC3** [target] Closing a course keeps its sessions, payments, fee structures, timeline and dues unchanged.
- **REQ-042.AC4** [target] A closed course rejects new sessions (REQ-033), new timeline entries (REQ-053) and new
  REGULAR payments (REQ-060). Each attempt → 409.
- **REQ-042.AC5** [target] An unknown course → 404.
