# Production data migration

Snapshot used for design: Neon `sonata` / `production`, dumped 2026-10-04.
Rows at that time: 600 students, 858 sessions, 17 courses, 180 fee_structure rows, 2,539 payments, 358 discounts,
5,516 fee_dues_tracker rows, 2 accounts.

### REQ-080 Legacy fee structures become templates and timeline entries
Status: accepted · Source: design, 2026-10-04

- **REQ-080.AC1** [retired] *(per-course design, replaced by REQ-082.AC1)* Each `(course, year)` group of legacy `fee_structure` rows becomes one template named
  `"<course> <year>"`, with `termsPerYear` = number of rows and the amounts copied. It also becomes one timeline entry
  from `<year>-01-01` to `<year>-12-31`.
- **REQ-080.AC2** [retired] *(replaced by REQ-082.AC2)* Every legacy REGULAR payment gets `periodDate` = the start date of its legacy term. No
  payment row is lost or changes amount, date, student, discounts or active flag.
- **REQ-080.AC3** [retired] *(replaced by REQ-082.AC3)* Legacy dues with status `STUDENT_LEFT_DUE_CANCELLED` become waivers.
- **REQ-080.AC4** [retired] *(replaced by REQ-082.AC4)* Legacy tables are renamed `legacy_*`, not dropped.
- **REQ-080.AC5** [target] Accounts with scope `ANY` get the `ADMIN` role and keep their passwords. Other accounts
  get no roles.
- **REQ-080.AC6** [target] Row counts of students, sessions, courses, payments and discounts are identical before
  and after the migration.

### REQ-081 Check the migrated dues against the old ones
Status: accepted · Source: user ("test everything including migration works properly"), 2026-10-04

- **REQ-081.AC1** [retired] *(replaced by REQ-083: a SQL report instead of an endpoint)* `GET /api/dues/legacy-comparison` (DUES_READ) compares, per student:
  - **Legacy billed:** the term amounts of legacy dues not `CANCELLED`.
  - **Legacy outstanding:** `due_amount` of legacy dues in `DUE` or `PARTIALLY_PAID`.
  - **New billed:** `amount` of the derived dues.
  - **New outstanding:** `balance` of the derived dues that are not `WAIVED`.

  It returns `{students: [{studentId, legacyBilled, newBilled, legacyOutstanding, newOutstanding, match}],
  totals: {legacyBilled, newBilled, legacyOutstanding, newOutstanding}, mismatches}`. → 404 once the legacy tables
  are gone.
- **REQ-081.AC2** [retired] *(replaced by REQ-083)* On the known legacy seed, every student matches, so `mismatches = 0` and the totals are
  equal.
- **REQ-081.AC3** [retired] *(replaced by REQ-083)* On the local copy of production:
  - the migration and a full rebuild finish with no errors;
  - every REGULAR payment has a `period_date`;
  - the number of waivers equals the number of legacy `STUDENT_LEFT_DUE_CANCELLED` dues;
  - the comparison report is produced, so its mismatches can be reviewed before production.

### REQ-082 Legacy fees become school-wide structures on the timeline
Status: accepted · Source: user + design, 2026-10-15

Production (checked 2026-10-15 on the local copy): 2024 had 3 terms of 4 months, 2025 and 2026 have 4 terms of 3
months, and each course charges the same amount in every term of a year. All 2,063 REGULAR payments have a course
that matches their legacy term.

- **REQ-082.AC1** [manual] Legacy `fee_structure` rows of each year become one structure named `Fees <year>` with
  `termMonths = 12 / number of terms that year` and that year's amount per course, plus a timeline entry on
  `<year>-01-01`. A year whose amounts and term length equal the previous year's reuses that structure (no new entry).
- **REQ-082.AC2** [manual] `payments.term_id` is dropped. No payment row is lost or changes amount, date, student,
  course, discounts or active flag.
- **REQ-082.AC3** [retired] Legacy cancellations became waivers. Replaced by REQ-082.AC6 (waivers were dropped).
- **REQ-082.AC4** [manual] The legacy tables are kept as `legacy_fee_structure` and `legacy_fee_dues`, read-only.
- **REQ-082.AC6** [manual] Each legacy due with status `STUDENT_LEFT_DUE_CANCELLED` (87 in production) becomes a ₹0
  REGULAR payment for that student and course, dated the legacy term's start, with one discount equal to the
  cancelled `due_amount` labelled "Forgiven: student left (migrated)".
- **REQ-082.AC5** [manual] After the migration, everyone's dues are calculated on first startup, and calculating
  again changes nothing.

### REQ-083 Check the new dues against the old ones before going live
Status: accepted · Source: user ("test everything including migration works properly"), 2026-10-04, reworked 2026-10-15

- **REQ-083.AC1** [process] `scripts/db/compare-dues.sql` lists, per student, the legacy outstanding amount (legacy
  dues in `DUE` or `PARTIALLY_PAID`) next to the new balance and credit, with totals and the students that differ.
- **REQ-083.AC2** [process] It is run on a fresh copy of production after the migration, and every difference is
  explained and signed off by the user before the release.
