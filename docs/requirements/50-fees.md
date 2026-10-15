# Fee structures and the fee timeline

A **fee structure** covers every subject: a name, a term length in months, and an amount per course. It has no dates.
The **fee timeline** is school-wide: each entry says "from this date, this structure applies". Terms are counted from
each entry's start date, and a new entry may only start on a term boundary of the one before it. Dues are calculated
from the timeline (see 70-dues.md).

## Retired (per-course design of 2026-10-04, never released)

The first design had one structure per course, `termsPerYear` with an amount per term, and a timeline per course
with free start and end dates. The user replaced it on 2026-10-15 with school-wide structures and a boundary-only
timeline (REQ-057 to REQ-059).

### REQ-050 Fee structures as templates
Status: superseded by REQ-057

- **REQ-050.AC1** [retired] `POST /api/fee-structures` `{name, courseName, termsPerYear, terms}`. Replaced by REQ-057.AC1.
- **REQ-050.AC2** [retired] `termsPerYear` must divide 12. Replaced by REQ-057.AC2.
- **REQ-050.AC3** [retired] One amount per term number. Replaced by REQ-057.AC1.
- **REQ-050.AC4** [retired] Amount checks. Replaced by REQ-057.AC3.
- **REQ-050.AC5** [retired] Unknown course / duplicate name per course. Replaced by REQ-057.AC3, AC4.
- **REQ-050.AC6** [retired] List by course with `usedInTimeline`. Replaced by REQ-057.AC5.
- **REQ-050.AC7** [retired] *(was baseline)* Legacy `POST /api/fee-structures` `{courseId, year, numberOfTerms,
  termFees}` with one row per term, and the `/by-*` lookups. Removed with the legacy term model; the data is
  migrated by REQ-082.

### REQ-051 Edit fee structures
Status: superseded by REQ-057

- **REQ-051.AC1** [retired] Edit allowed while in use. Replaced by REQ-057.AC6.
- **REQ-051.AC2** [retired] An edit rebuilds the course's dues. Replaced by REQ-057.AC7.
- **REQ-051.AC3** [retired] A structure's course can't change. Structures no longer belong to a course.

### REQ-052 Clone and delete fee structures
Status: superseded by REQ-057

- **REQ-052.AC1** [retired] Clone. Dropped: create a new structure instead.
- **REQ-052.AC2** [retired] Delete only when unused. Replaced by REQ-057.AC8.

### REQ-053 Fee timeline
Status: superseded by REQ-058

- **REQ-053.AC1** [retired] Per-course entries with `effectiveTo`. Replaced by REQ-058.AC1.
- **REQ-053.AC2** [retired] Structure must belong to the course. Replaced by REQ-058.AC2.
- **REQ-053.AC3** [retired] No overlaps per course. Replaced by REQ-058.AC1, AC3.
- **REQ-053.AC4** [retired] List per course. Replaced by REQ-058.AC4.
- **REQ-053.AC5** [retired] Edit and delete entries. Replaced by REQ-058.AC5.
- **REQ-053.AC6** [retired] Gaps allowed. The timeline is now contiguous from its first entry.
- **REQ-053.AC7** [retired] Closed courses. Not part of this design.

### REQ-054 Billing periods
Status: superseded by REQ-059

- **REQ-054.AC1** [retired] Calendar-year blocks of 12/N months. Replaced by REQ-059.AC1 (terms count from the entry).
- **REQ-054.AC2** [retired] The entry active on a period's start bills it. Replaced by REQ-058.AC2 (changes only at
  term boundaries, so no period is split).
- **REQ-054.AC3** [retired] Cursor rules for mid-term changes. Not needed: REQ-058.AC2.
- **REQ-054.AC4** [retired] Short periods billed in full. Not needed: REQ-058.AC2.

### REQ-055 Timeline preview
Status: superseded by REQ-059

- **REQ-055.AC1** [retired] Preview per course. Replaced by REQ-059.AC2.

### REQ-056 Timeline changes rebuild dues
Status: superseded by REQ-058

- **REQ-056.AC1** [retired] Rebuild the course. Replaced by REQ-058.AC6 (everyone is recalculated).

## Current design

### REQ-057 Fee structures cover all subjects
Status: accepted · Source: user ("fee structure name, each subject how much, and how many months is a term"), 2026-10-15

- **REQ-057.AC1** [target] `POST /api/fee-structures` `{name, termMonths, amounts: [{courseName, amount}]}` → 201
  with `{id, name, termMonths, amounts, usedInTimeline: false}`. Amounts are listed by course name.
- **REQ-057.AC2** [target] `termMonths` must be a whole number from 1 to 12 → otherwise 400.
- **REQ-057.AC3** [target] A blank name, a negative amount, an unknown course or the same course twice → 400 with a
  `details` entry for the field. An amount of 0 is allowed. A course left out is simply not billed while that
  structure applies.
- **REQ-057.AC4** [target] A duplicate name (ignoring case) → 409.
- **REQ-057.AC5** [target] `GET /api/fee-structures` lists structures ordered by name, each with `usedInTimeline`.
  `GET /api/fee-structures/{id}` → 200, or 404.
- **REQ-057.AC6** [target] `PUT /api/fee-structures/{id}` replaces name, `termMonths` and amounts, with the same
  checks, even while the structure is on the timeline. If a new `termMonths` would put a later timeline entry off a
  term boundary (REQ-058.AC2) → 409, and nothing changes.
- **REQ-057.AC7** [target] Editing a structure that is on the timeline recalculates everyone's dues (REQ-074.AC7).
- **REQ-057.AC8** [target] `DELETE /api/fee-structures/{id}` → 204 when it isn't on the timeline, 409 when it is,
  404 when unknown.
- **REQ-057.AC9** [target] `PUT /api/fee-structures/{id}` returns the structure plus `recalculation` (REQ-058.AC10)
  when it is on the timeline, and `recalculation: null` when it isn't. `?dryRun=true` validates and calculates the
  same impact but saves nothing → 200 with `dryRun: true`; the structure and the dues stay as they were.

### REQ-058 One school-wide timeline, changed only at term boundaries
Status: accepted · Source: user ("at what time follow which fee structure"; "a new fee structure can come in force only at the beginning of the term"), 2026-10-15

- **REQ-058.AC1** [target] `POST /api/fee-timeline` `{feeStructureId, effectiveFrom}` → 201 with
  `{id, feeStructureId, feeStructureName, termMonths, effectiveFrom, effectiveTo}`. An entry runs until the day before
  the next entry starts; the last one is open-ended (`effectiveTo: null`). Nothing is billed before the first entry.
- **REQ-058.AC2** [target] An entry's `effectiveFrom` must be a term boundary of the entry before it: that entry's
  `effectiveFrom` plus a whole number (≥ 1) of its `termMonths`. Otherwise → 400, and the message names the nearest
  valid dates before and after. The entry after it must also stay on a boundary of the new entry → otherwise 400.
- **REQ-058.AC3** [target] Two entries on the same date → 409. An unknown structure → 400.
- **REQ-058.AC4** [target] `GET /api/fee-timeline` lists the entries ordered by `effectiveFrom`, with `effectiveTo`
  filled in.
- **REQ-058.AC5** [retired] `DELETE /api/fee-timeline/{id}` → 204. Replaced by REQ-058.AC9 (the delete reports its
  recalculation).
- **REQ-058.AC6** [target] Every timeline change recalculates everyone's dues (REQ-074.AC7).
- **REQ-058.AC9** [target] `PUT /api/fee-timeline/{id}` changes the structure or the date and `DELETE
  /api/fee-timeline/{id}` → 200 `{dryRun, recalculation}`, both with the boundary checks of AC2 for the entries around
  it. An unknown id → 404.
- **REQ-058.AC10** [target] Add, change and delete return `recalculation: {students, inserted, updated, deleted,
  outstandingBefore, outstandingAfter, outstandingChange, durationMs}`: how many students' dues changed, how many due
  rows were inserted, updated and deleted, and the total outstanding balance before and after. *(user: the frontend
  shows what happened)*
- **REQ-058.AC11** [target] `?dryRun=true` on add, change and delete runs every check and the whole recalculation, then
  saves nothing → 200 with `dryRun: true` and the same `recalculation` the real change would report. The timeline and
  the dues stay as they were. *(user: double confirmation before the heavy recalculation)*

### REQ-059 Terms
Status: accepted · Source: user ("how many months is a term", terms counted from the timeline entry), 2026-10-15

- **REQ-059.AC1** [target] An entry with structure S starting on date F produces terms of S's `termMonths`: the
  first runs from F to the day before F + termMonths, the next starts there, and so on until the next entry. Month
  arithmetic keeps the day of month; a start on the 29th–31st uses the last day of shorter months.
- **REQ-059.AC2** [target] `GET /api/fee-timeline/preview?from=&to=` → the terms that overlap `[from, to]`:
  `[{periodStart, periodEnd, feeStructureId, feeStructureName, amounts: [{courseName, amount}]}]`, ordered by
  `periodStart`. It writes nothing. A missing or invalid `from` or `to`, or `from` after `to` → 400.
