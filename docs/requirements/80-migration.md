# Production data migration

Snapshot used for design: Neon `sonata` / `production`, dumped 2026-10-04.
Rows at that time: 600 students, 858 sessions, 17 courses, 180 fee_structure rows, 2,539 payments, 358 discounts,
5,516 fee_dues_tracker rows, 2 accounts.

### REQ-080 Legacy fee structures become templates and timeline entries
Status: accepted · Source: design, 2026-10-04

- **REQ-080.AC1** [target] Each `(course, year)` group of legacy `fee_structure` rows becomes one template named
  `"<course> <year>"`, with `termsPerYear` = number of rows and the amounts copied. It also becomes one timeline entry
  from `<year>-01-01` to `<year>-12-31`.
- **REQ-080.AC2** [target] Every legacy REGULAR payment gets `periodDate` = the start date of its legacy term. No
  payment row is lost or changes amount, date, student, discounts or active flag.
- **REQ-080.AC3** [target] Legacy dues with status `STUDENT_LEFT_DUE_CANCELLED` become waivers.
- **REQ-080.AC4** [target] Legacy tables are renamed `legacy_*`, not dropped.
- **REQ-080.AC5** [target] Accounts with scope `ANY` get the `ADMIN` role. Other accounts get no roles.
- **REQ-080.AC6** [target] Row counts of students, sessions, courses, payments and discounts are identical before
  and after the migration.

### REQ-081 Check the migrated dues against the old ones
Status: accepted · Source: user ("test everything including migration works properly"), 2026-10-04

- **REQ-081.AC1** [target] A dry-run rebuild on the migrated prod copy produces a report comparing each student's
  derived dues with `legacy_fee_dues_tracker` (period, amount, balance, status).
- **REQ-081.AC2** [target] For the same students, total billed amounts and total collected amounts match legacy,
  except for differences listed and explained in the report.
- **REQ-081.AC3** [target] The full migration and rebuild runs on the local prod copy without errors before it is
  run on production.
