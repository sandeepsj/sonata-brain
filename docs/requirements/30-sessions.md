# Student sessions

A session is a student's enrolment in a course from `dateOfJoining` to `dateOfDisjoining` (null means ongoing).

### REQ-030 Create a session
Status: verified · Source: existing code

- **REQ-030.AC1** [baseline] `POST /api/student-sessions` `{studentId, courseName, dateOfJoining?,
  dateOfDisjoining?}` → 201 (200 before REQ-030.AC7) with the session. A missing `dateOfJoining` defaults to now.
- **REQ-030.AC2** [baseline] An unknown student or course → 404.
- **REQ-030.AC3** [baseline] A `dateOfJoining` or `dateOfDisjoining` in the future → 400.
- **REQ-030.AC4** [baseline] A `dateOfDisjoining` before `dateOfJoining` → 400.
- **REQ-030.AC5** [baseline] A session overlapping an existing session of the same student in the same course → 409,
  and the message names the conflicting session. The same student may hold sessions in different courses at the same
  time.
- **REQ-030.AC6** [baseline] A missing `studentId` or `courseName` → 400.
- **REQ-030.AC7** [target] Success → 201. An overlap → 409.
- **REQ-030.AC8** [target] Creating a session triggers a dues rebuild for that student (REQ-074).

### REQ-031 Disjoin a session
Status: verified · Source: existing code

- **REQ-031.AC1** [baseline] `PATCH /api/student-sessions/{id}/disjoin` sets `dateOfDisjoining` to now → 200.
- **REQ-031.AC2** [baseline] A session that is already disjoined → 409 (400 before REQ-031.AC3). An unknown session
  → 404.
- **REQ-031.AC3** [target] An optional body `{date}` sets a past disjoin date, which can't be before
  `dateOfJoining` → 400. An already disjoined session → 409.
- **REQ-031.AC4** [target] Disjoining triggers a dues rebuild for that student (REQ-074).

### REQ-032 Query sessions
Status: verified · Source: existing code

- **REQ-032.AC1** [baseline] `GET /api/student-sessions/{id}` → 200, or 404 if unknown.
- **REQ-032.AC2** [retired] *(was baseline)* `GET /api/student-sessions/all?studentId=&courseName=&active=`.
  Replaced by REQ-032.AC7.
- **REQ-032.AC3** [retired] *(was baseline)* `/active/student/{id}` and `/all/student/{id}` returned 404 when
  nothing matched. Replaced by REQ-032.AC6 and AC7.
- **REQ-032.AC4** [retired] *(was baseline)* `/active/course/{name}` and `/all/course/{name}` returned 404 when
  nothing matched. Replaced by REQ-032.AC6 and AC7.
- **REQ-032.AC5** [retired] *(was baseline)* `/search` (by course, year, term) needed a filter. Removed: it depended
  on the legacy per-year fee rows. Use REQ-032.AC7 (`activeOn`) instead.
- **REQ-032.AC6** [target] List endpoints return 200 with `[]` when nothing matches. 404 is only for an unknown
  student or course.
- **REQ-032.AC7** [target] `GET /api/student-sessions?studentId=&courseName=&active=&activeOn=<date>` replaces the
  `/all`, `/active/*` and `/students/*` variants. `activeOn` returns the sessions that overlap that date.

### REQ-033 Closed courses take no new enrolments
Status: verified · Source: user, 2026-10-04

- **REQ-033.AC1** [target] Creating a session in a `CLOSED` course → 409. Existing sessions are unaffected.
