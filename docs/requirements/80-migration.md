# Production data migration

Snapshot used for design: Neon `sonata` / `production`, dumped 2026-10-04.
Rows at that time: 600 students, 858 sessions, 17 courses, 180 fee_structure rows, 2,539 payments, 358 discounts,
5,516 fee_dues_tracker rows, 2 accounts.

### REQ-080 Legacy fee structures become templates and timeline entries
Status: verified · Source: design, 2026-10-04

- **REQ-080.AC1** [target] Each `(course, year)` group of legacy `fee_structure` rows becomes one template named
  `"<course> <year>"`, with `termsPerYear` = number of rows and the amounts copied. It also becomes one timeline entry
  from `<year>-01-01` to `<year>-12-31`.
- **REQ-080.AC2** [target] Every legacy REGULAR payment gets `periodDate` = the start date of its legacy term. No
  payment row is lost or changes amount, date, student, discounts or active flag.
- **REQ-080.AC3** [target] Legacy dues with status `STUDENT_LEFT_DUE_CANCELLED` become waivers.
- **REQ-080.AC4** [target] Legacy tables are renamed `legacy_*`, not dropped.
- **REQ-080.AC5** [target] Accounts with scope `ANY` get the `ADMIN` role and keep their passwords. Other accounts
  get no roles.
- **REQ-080.AC6** [target] Row counts of students, sessions, courses, payments and discounts are identical before
  and after the migration.

### REQ-081 Check the migrated dues against the old ones
Status: verified · Source: user ("test everything including migration works properly"), 2026-10-04

- **REQ-081.AC1** [target] `GET /api/dues/legacy-comparison` (DUES_READ) compares, per student:
  - **Legacy billed:** the term amounts of legacy dues not `CANCELLED`.
  - **Legacy outstanding:** `due_amount` of legacy dues in `DUE` or `PARTIALLY_PAID`.
  - **New billed:** `amount` of the derived dues.
  - **New outstanding:** `balance` of the derived dues that are not `WAIVED`.

  It returns `{students: [{studentId, legacyBilled, newBilled, legacyOutstanding, newOutstanding, match}],
  totals: {legacyBilled, newBilled, legacyOutstanding, newOutstanding}, mismatches}`. → 404 once the legacy tables
  are gone.
- **REQ-081.AC2** [target] On the known legacy seed, every student matches, so `mismatches = 0` and the totals are
  equal.
- **REQ-081.AC3** [target] On the local copy of production:
  - the migration and a full rebuild finish with no errors;
  - every REGULAR payment has a `period_date`;
  - the number of waivers equals the number of legacy `STUDENT_LEFT_DUE_CANCELLED` dues;
  - the comparison report is produced, so its mismatches can be reviewed before production.
