# Dues

Dues are **derived**: dues = sessions × fee timeline − active REGULAR payments − waivers. The `fee_due` table is a
projection. It can be deleted and rebuilt at any time without losing information.

### REQ-070 Dues are derived and rebuildable
Status: accepted · Source: user ("recalculate dues for the entire institute whenever needed"), 2026-10-04

- **REQ-070.AC1** [target] A student is billed for every billing period (REQ-054) of their course that overlaps a
  session, up to the horizon (REQ-075). One due per (session, period).
- **REQ-070.AC2** [target] Each due exposes `{id, studentId, studentName, sessionId, courseName, periodStart,
  periodEnd, termNumber, termsPerYear, feeStructureId, amount, paidAmount, discountAmount, balance, status}`.
  `balance = amount − paidAmount − discountAmount`, and is never below 0.
- **REQ-070.AC3** [target] Status is `DUE` if nothing has been applied, `PARTIALLY_PAID` if 0 < applied < amount,
  `PAID` if applied = amount, `OVERPAID` if applied > amount (the excess is shown as `overpaidAmount`), and `WAIVED` if
  a waiver exists.
- **REQ-070.AC4** [target] Running a rebuild twice in a row changes nothing (same rows, same values).
- **REQ-070.AC5** [target] Deleting every `fee_due` row and rebuilding everything reproduces identical dues.

### REQ-071 Applying payments to dues
Status: accepted · Source: design, 2026-10-04

- **REQ-071.AC1** [target] An active REGULAR payment is applied to the due of the same student and course whose
  period contains `periodDate`.
- **REQ-071.AC2** [target] If no such due exists, for example because the timeline changed shape, the payment is
  applied to the oldest due of that course with a balance above 0. Any remainder moves to the next one.
- **REQ-071.AC3** [target] Money left after every due of that course is fully paid stays on the latest due as
  `OVERPAID`. It is never lost.
- **REQ-071.AC4** [target] Several payments for one period add up. Discounts count towards the applied amount.
- **REQ-071.AC5** [target] Inactive payments and ADDITIONAL payments are ignored.

### REQ-072 Waivers
Status: accepted · Source: existing code (`mark-student-left-cancelled`), reworked 2026-10-04

- **REQ-072.AC1** [baseline] `PATCH /api/dues/{id}/mark-student-left-cancelled` → 200 with
  `status: STUDENT_LEFT_DUE_CANCELLED`, but only for dues in `DUE` or `PARTIALLY_PAID` whose student has a disjoined
  session in that course. Otherwise → 400.
- **REQ-072.AC2** [target] `POST /api/dues/waivers` `{sessionId, periodStart, reason}` → 201, and that due becomes
  `WAIVED` with `balance: 0`. The session must be disjoined → otherwise 409. An existing waiver for the same
  (session, period) → 409.
- **REQ-072.AC3** [target] Waivers survive rebuilds. `DELETE /api/dues/waivers/{id}` → 204, and the due returns to
  its computed status.
- **REQ-072.AC4** [target] `GET /api/dues/waivers?studentId=` lists waivers with their reason, author and date.

### REQ-073 Fee changes leave earlier periods alone
Status: accepted · Source: user ("fee structure updates in middle of an year, but keep the fee dues still intact"), 2026-10-04

- **REQ-073.AC1** [target] Adding a timeline entry from date D changes only dues for periods starting on or after D.
  Earlier periods keep the same amount, payments and status.
- **REQ-073.AC2** [target] Payments already made for periods that are re-billed stay applied by their `periodDate`
  (REQ-071). No payment record changes.

### REQ-074 When dues are rebuilt
Status: accepted · Source: user + design, 2026-10-04

- **REQ-074.AC1** [target] Dues for a student are rebuilt after their session is created or disjoined, and after
  their payment is created, deactivated or converted.
- **REQ-074.AC2** [target] Dues for a course are rebuilt after a timeline change or an edit to a structure used in
  that course's timeline.
- **REQ-074.AC3** [target] `POST /api/dues/rebuild?scope=ALL|COURSE|STUDENT&id=&dryRun=` → 202 `{jobId}`.
  `GET /api/dues/rebuild/{jobId}` → `{status: RUNNING|DONE|FAILED, created, updated, deleted, errors[]}`.
- **REQ-074.AC4** [target] `dryRun=true` writes nothing and returns the differences it would make.
- **REQ-074.AC5** [target] A failure for one student is recorded in `errors` and doesn't stop the others.
- **REQ-074.AC6** [target] A nightly job rebuilds everything, so dues for new periods appear without anyone acting.

### REQ-075 How far ahead dues are created
Status: accepted · Source: design (matches today: whole current year), 2026-10-04

- **REQ-075.AC1** [target] Dues are created for periods starting on or before 31 December of the current year.

### REQ-076 Querying dues
Status: accepted · Source: existing code; contract revised 2026-10-04

- **REQ-076.AC1** [baseline] `GET /api/dues/student/{id}` lists the student's dues `{id, studentId, studentName,
  sessionId, courseName, feeStructureId, termYear, termNumber, dueAmount, status}`, ordered by year and term.
- **REQ-076.AC2** [baseline] `GET /api/dues/summary/{studentId}` returns per-status counts and amounts, plus
  `totalDueAmount` (DUE + PARTIALLY_PAID).
- **REQ-076.AC3** [baseline] `GET /api/dues?statuses=&year=&termNumber=&course=`,
  `/pending`, `/aggregate` and `/aggregate/by-course` filter non-cancelled dues. An invalid status → 400.
- **REQ-076.AC4** [target] `GET /api/dues?studentId=&courseName=&status=&from=&to=` (by period start), ordered by
  student and then period start.
- **REQ-076.AC5** [target] `GET /api/dues/summary?studentId=|courseName=` returns totals: `{amount, paid, discount,
  balance, overpaid}` plus a count per status.
- **REQ-076.AC6** [target] `GET /api/dues/aggregate?groupBy=course|month|status&from=&to=` returns grouped
  `{key, count, amount, balance}`.
