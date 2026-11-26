# Students

### REQ-020 Create a student
Status: accepted · Source: existing code; admission-number override added in commit bdcd6ff

- **REQ-020.AC1** [baseline] `POST /api/students` with `studentName` and no `admissionNo` → 200, with
  `admissionNo` = current maximum + 1 (1 on an empty table).
- **REQ-020.AC2** [baseline] With an explicit `admissionNo` that is not taken → 200, and that number is used.
- **REQ-020.AC3** [baseline] With an `admissionNo` that already exists → 400.
- **REQ-020.AC4** [baseline] A missing or blank `studentName` → 400.
- **REQ-020.AC5** [baseline] A missing `admissionDate` defaults to today. `status` defaults to `active`.
- **REQ-020.AC6** [baseline] All provided fields are stored and returned: `contactNo`, `dob`, `className`,
  `school`, `address`, `pinCode`, `remarks`.
- **REQ-020.AC7** [target] Success returns 201. A duplicate `admissionNo` → 409 and a blank name → 400, both with the
  standard error body (REQ-005).

### REQ-021 Get and update a student
Status: accepted · Source: existing code

- **REQ-021.AC1** [baseline] `GET /api/students/{admissionNo}` → 200 with the student.
- **REQ-021.AC2** [baseline] An unknown admission number → 404.
- **REQ-021.AC3** [baseline] `PUT /api/students/{admissionNo}` updates only the fields that are present and
  non-null. A blank `studentName` is ignored. `admissionNo` and `admissionDate` cannot be changed this way.
- **REQ-021.AC4** [baseline] `PUT` for an unknown admission number → 404.
- **REQ-021.AC5** [baseline] `status` can be changed to `active`, `closed` or `suspended`.

### REQ-022 List students
Status: accepted · Source: existing code

- **REQ-022.AC1** [baseline] `GET /api/students?page=&size=` returns a Spring page (`content`, `totalElements`,
  `totalPages`, `number`, `size`). The defaults are page 0 and size 10.

### REQ-023 Search students
Status: accepted · Source: existing code; admission-number search added in commit f8edee0

- **REQ-023.AC1** [baseline] `GET /api/students/search` with no parameters returns all students.
- **REQ-023.AC2** [baseline] `name` and `school` match case-insensitive substrings. `contactNo` matches a substring.
- **REQ-023.AC3** [baseline] `className` matches case-insensitively but must be exact. `pinCode` and `admissionNo`
  must match exactly.
- **REQ-023.AC4** [baseline] `admissionDateFrom` and `admissionDateTo` are inclusive. Either can be used alone.
- **REQ-023.AC5** [baseline] Multiple filters are combined with AND.

### REQ-024 Filter student search by status and course
Status: verified · Source: user request (2026-11-25), Students page needs Subject and Status filters

- **REQ-024.AC1** [target] `GET /api/students/search` accepts `status` (`active`, `closed` or `suspended`). It must
  match exactly. Any other value, including different letter case → 400.
- **REQ-024.AC2** [target] `GET /api/students/search` accepts `courseName`. It keeps students who have a session in
  that course that is active now (`dateOfDisjoining` is empty or in the future, the same rule as
  `GET /api/student-sessions?active=true`). The name is case-sensitive. A student with
  several such sessions is returned once. An unknown course returns `[]`, not 404.
- **REQ-024.AC3** [target] Both combine with the other REQ-023 filters using AND.
