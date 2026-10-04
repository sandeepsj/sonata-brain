# Requirements log

Single source of truth for what the Sonata Brain backend must do. Code and tests follow this log, not the other way round.

## Files

| File | Contents |
|---|---|
| [HISTORY.md](HISTORY.md) | Append-only, dated record of every requirement added, changed, or dropped, and who asked for it. |
| [00-platform.md](00-platform.md) | Process and cross-cutting rules: API-only scope, tests-first, error format, migrations, prod data. |
| [10-access.md](10-access.md) | Login, roles, per-resource permissions. |
| [20-students.md](20-students.md) | Students. |
| [30-sessions.md](30-sessions.md) | Student sessions (enrolment in a course). |
| [40-courses.md](40-courses.md) | Courses: create, close, reopen. |
| [50-fees.md](50-fees.md) | Fee structures (templates) and the fee timeline. |
| [60-payments.md](60-payments.md) | Payments, discounts, deactivation, conversion. |
| [70-dues.md](70-dues.md) | Derived dues, rebuilds, waivers, queries. |
| [80-migration.md](80-migration.md) | Moving production data to the new model. |

## Format

Each requirement has a stable ID (`REQ-nnn`) that is never reused, a status, a source and acceptance criteria:

```
### REQ-042 Close and reopen a course
Status: accepted · Source: user, 2026-10-04 · Since: v2

- **REQ-042.AC1** [target] `PATCH /api/courses/{name}/close` → 200, `status=CLOSED`, `closedAt` set.
```

- **Status:** `proposed` (needs the user's confirmation), `accepted`, `implemented`, `verified` (target tests green),
  `superseded by REQ-xxx`, `dropped`.
- **Criterion tags:**
  - `[baseline]` describes how the API behaves today. Its contract case must pass on the current code. It protects
    behaviour during the rewrite.
  - `[target]` describes the new contract. Its contract case fails until the feature is built.
  - `[retired]` was `[baseline]` until the new contract replaced it. The criterion stays for history and names its
    replacement. Its case was deleted, and coverage doesn't count it.
  - `[process]` describes how the project is run (scripts, logs, test format). It can't be verified through the
    API, so contract coverage doesn't count it.
  - A criterion that exists only to record a current defect is tagged `[baseline-defect]`. Its case documents the
    current behaviour, and the criterion names the `[target]` criterion that fixes it. When the fix lands, the
    baseline case is deleted in the same commit, and that is noted in HISTORY.md.

## Rules

1. A new or changed requirement goes into HISTORY.md first (date, source, what changed), then into its area file.
2. Every acceptance criterion is proved by at least one case in [`contract/specs/`](../../contract/README.md),
   which cites it under `req:`. The contract runner reports uncovered criteria and fails on unknown citations.
3. IDs are never renumbered or reused. Dropped requirements stay in the file, marked `dropped`.
