# Fee structures and the fee timeline

A **fee structure** is a reusable template for one course: a name, `termsPerYear` and an amount for each term. It
has no dates. The **fee timeline** assigns structures to date ranges for a course. Dues are computed from the timeline
(see 70-dues.md).

### REQ-050 Fee structures as templates
Status: accepted · Source: user ("set up different fee structures independently"), 2026-10-04

- **REQ-050.AC1** [target] `POST /api/fee-structures` `{name, courseName, termsPerYear, terms: [{termNumber,
  amount}]}` → 201 with `id`.
- **REQ-050.AC2** [target] `termsPerYear` must divide 12 (1, 2, 3, 4, 6 or 12), otherwise → 400.
- **REQ-050.AC3** [target] `terms` must contain each `termNumber` from 1 to `termsPerYear` exactly once → otherwise
  400, and the message lists the missing or duplicate term numbers.
- **REQ-050.AC4** [target] A negative amount → 400. An amount of 0 is allowed.
- **REQ-050.AC5** [target] An unknown course → 404. A duplicate name within the same course → 409.
- **REQ-050.AC6** [target] `GET /api/fee-structures?courseName=` lists structures, each with its terms and a
  `usedInTimeline` flag. `GET /api/fee-structures/{id}` returns one, or 404.
- **REQ-050.AC7** [baseline] Today: `POST /api/fee-structures` `{courseId, year, numberOfTerms, termFees:[{term,
  amount}]}` → 200 with one row per term. A second structure for the same course and year → 400. Missing terms → 400.
  An unknown course → 400.

### REQ-051 Edit fee structures
Status: accepted · Source: user ("update the fee"; "keep data independent … recalculate dues whenever needed"), 2026-10-04

- **REQ-051.AC1** [target] `PUT /api/fee-structures/{id}` replaces `name`, `termsPerYear` and `terms`, with the same
  validation as REQ-050. This is allowed even when the structure is used in the timeline.
- **REQ-051.AC2** [target] Editing a structure that is used in the timeline rebuilds dues for its course (REQ-074).
  Payments are never changed.
- **REQ-051.AC3** [target] The course of a structure cannot be changed → 400 if `courseName` differs.

### REQ-052 Clone and delete fee structures
Status: accepted · Source: design, 2026-10-04

- **REQ-052.AC1** [target] `POST /api/fee-structures/{id}/clone` `{name}` → 201 with a new structure that has the
  same course, terms and amounts.
- **REQ-052.AC2** [target] `DELETE /api/fee-structures/{id}` → 204 if the structure isn't used in the timeline, 409
  if it is, 404 if unknown.

### REQ-053 Fee timeline
Status: accepted · Source: user ("assign each of those to the time period they wish"), 2026-10-04

- **REQ-053.AC1** [target] `POST /api/fee-timeline` `{courseName, feeStructureId, effectiveFrom, effectiveTo?}` →
  201. A null `effectiveTo` means open-ended.
- **REQ-053.AC2** [target] The structure must belong to the same course → otherwise 400. `effectiveFrom` must be on
  or before `effectiveTo` → otherwise 400.
- **REQ-053.AC3** [target] Entries for one course may not overlap. Both ends are inclusive, and an open end counts as
  infinite. An overlap → 409, and the message names the conflicting entry.
- **REQ-053.AC4** [target] `GET /api/fee-timeline?courseName=` lists entries ordered by `effectiveFrom`, each with
  the structure's name.
- **REQ-053.AC5** [target] `PUT /api/fee-timeline/{id}` changes the dates or the structure, with the same rules.
  `DELETE /api/fee-timeline/{id}` → 204.
- **REQ-053.AC6** [target] Gaps between entries are allowed. Periods that fall in a gap produce no dues.
- **REQ-053.AC7** [target] Adding an entry to a CLOSED course → 409 (REQ-042.AC4).

### REQ-054 Billing periods
Status: accepted · Source: user ("this fee structure is activated from this day to this day"; mid-term rule chosen 2026-10-04)

- **REQ-054.AC1** [target] A structure with N terms per year splits each calendar year into N blocks of 12/N months.
  Term k runs from the 1st of month (k−1)·12/N+1 to the last day of month k·12/N.
- **REQ-054.AC2** [target] A period is billed by the timeline entry that is active on the period's **start date**,
  using that entry's structure. That structure decides the period length and amount.
- **REQ-054.AC3** [target] Periods are generated with a cursor and never overlap or repeat:
  1. Start at the course's earliest `effectiveFrom`.
  2. Take the structure from the entry active on the cursor date.
  3. The period runs from the cursor to the end of that structure's term containing the cursor.
  4. The next cursor is the day after.
  5. If the cursor falls in a gap, it jumps to the next entry's `effectiveFrom`.

  Example: quarterly from 1 Jan, monthly from 15 Feb. Q1 (Jan–Mar) is billed quarterly because quarterly is active on
  1 Jan. Then April, May and so on are billed monthly.
- **REQ-054.AC4** [target] *(proposed, needs your confirmation)* If the cursor isn't on a term boundary of the active
  structure, the short period up to that term's end is billed at the **full** term amount, the same "no proration"
  rule that already applies to students who join mid-term. That happens when the timeline starts mid-term, when the
  previous period ends mid-term in the new structure, or after a gap. Example: monthly until
  28 Feb, then a 2-term structure (Jan–Jun, Jul–Dec) from 1 Mar. The period 1 Mar–30 Jun is charged the full term-1
  amount. The alternatives are prorating by days, or not billing the partial period and starting at 1 Jul.

### REQ-055 Timeline preview
Status: accepted · Source: design, 2026-10-04

- **REQ-055.AC1** [target] `GET /api/fee-timeline/preview?courseName=&from=&to=` returns the billing periods
  `[{periodStart, periodEnd, termNumber, termsPerYear, amount, feeStructureId}]` that the current timeline produces
  and that overlap `[from, to]`, ordered by `periodStart`. It doesn't write anything. A missing `from` or `to` → 400.

### REQ-056 Timeline changes rebuild dues
Status: accepted · Source: user ("every time this fee structure timeline changes, due recalculation triggers"), 2026-10-04

- **REQ-056.AC1** [target] Creating, updating or deleting a timeline entry rebuilds the dues of every student with a
  session in that course (REQ-074).
