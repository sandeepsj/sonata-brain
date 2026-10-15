# Requirements history

Append-only. Newest entries at the bottom. Each entry gives the date, the source and the requirements it touched.

## 2026-10-04: Initial requirements (conversation with the user)

| # | What was asked or decided | Source | Requirements |
|---|---|---|---|
| 1 | Users can add new courses ("subjects"), mark some as closed, update fees and start a new fee structure. | User | REQ-040, REQ-042, REQ-050, REQ-051 |
| 2 | Only some users may do these things: route-based access with separate read and write rights. | User | REQ-011 |
| 3 | Fee structures change mid-year, but existing dues must stay intact. | User | REQ-073 |
| 4 | Fee structures are set up independently and assigned to date ranges on a timeline. Every timeline change recalculates dues. | User | REQ-050, REQ-053, REQ-056 |
| 5 | Staff sometimes record a course-fee payment as "additional". It must be possible to convert it later. | User | REQ-065 |
| 6 | Scope is the backend and API only. No backward compatibility is needed; the frontend follows the new contract. | User | REQ-001 |
| 7 | A timeline boundary that falls mid-term is resolved by "the structure active on the term's start date bills the whole term". | User (option chosen) | REQ-054.AC2 |
| 8 | Payments, sessions and structures are independent facts. Dues are recalculated from them for the whole institute whenever needed, and fee structures stay editable. | User | REQ-051, REQ-070 |
| 9 | Permissions are per resource. Named roles group them. | User | REQ-011, REQ-012 |
| 10 | Existing production data is migrated, not discarded. | User | REQ-080, REQ-081 |
| 11 | ADDITIONAL payments may have no course. Only REGULAR payments need a course and period. | User question, design answer | REQ-061.AC3, REQ-061.AC5, REQ-065.AC2 |
| 12 | Rewrite in place rather than start a new repository. | User | — |
| 13 | Write an exhaustive test suite and keep this requirements log before writing feature code. Test today's behaviour first, then refine it. | User | REQ-002, REQ-003 |
| 14 | Download production into a local copy, keep the local DB resettable to an exact prod copy, and test the migration on it. | User | REQ-004, REQ-081.AC3 |
| 15 | The test suite must not be tied to Java. Test cases are YAML files: requests, data and expected results all written there, readable without reading any code, with a Python runner that holds no assertions. The database may be accessed for setup and resets. | User | REQ-002, REQ-003.AC1 |

### Design decisions recorded the same day (open to change)

- Dues are a derived projection with a nightly rebuild, and the horizon is the end of the current year (REQ-074,
  REQ-075).
- Payments link to a term by `periodDate` and no longer by a fee-structure ID (REQ-060.AC5, REQ-071).
- Waivers replace `STUDENT_LEFT_DUE_CANCELLED` (REQ-072).
- One error format across the API (REQ-005), Flyway migrations (REQ-006), bootstrap admin (REQ-015).

### Open questions

- **REQ-054.AC4:** how to bill a partial term left after a structure switch. Options are full amount (proposed),
  prorate by days, or skip to the next boundary.
- **REQ-005, REQ-015:** proposed by design and awaiting the user's confirmation.

## 2026-10-04: Findings while characterising today's behaviour

- REQ-063.AC2 corrected: discount fields are not validated today (the list lacks `@Valid`). Recorded as a known
  defect; fix specified as REQ-063.AC4.
- REQ-077 added: how dues are generated today (session, fee structure, payment, deactivation, disjoin). Note:
  creating a fee structure bills future terms immediately, but creating a session only bills terms up to today.
  Superseded by the derived-dues design, and kept as the comparison point for the migration.
- REQ-060.AC11 added: a REGULAR payment without a label returns 500 (NOT NULL column, prod too). Proposed fix
  REQ-060.AC12: label required for all payments.
- REQ-064.AC6 added: race between payment deactivation and the background dues job (shared lazy `discounts`
  collection), giving an intermittent 500 or an unrestored due. Reproduced in 5 of 6 runs. Fix specified as
  REQ-064.AC7. The affected baseline cases moved to a new `quarantine` suite.

## 2026-10-04: Target contract written

- Target specs added for access, platform, students, courses, sessions, fee structures, fee timeline and billing,
  payments, dues and the migration (seeded legacy database and a local production rehearsal).
- REQ-054.AC3/AC4 made consistent: billing starts on `effectiveFrom`, and any partial term (start of timeline,
  after a switch or after a gap) is billed at the full term amount. AC4 is still proposed.
- REQ-071.AC2 clarified: a payment fills its own period first, then the oldest unpaid dues, in payment-date order.
- REQ-012.AC2: roles list `?name=` filter, `builtIn` flag, all resources listed (NONE where not granted).
- REQ-066.AC4: filter semantics defined (`from`/`to` on payment date; `periodDate` = the billing period containing
  that date).
- REQ-074.AC6: nightly schedule readable at `GET /api/dues/rebuild/schedule`.
- REQ-081.AC1: legacy comparison endpoint and its definitions of billed and outstanding.
- REQ-015.AC1 changed: the bootstrap admin is created if its email doesn't exist, rather than only when no admin
  exists, so it also works on a migrated production copy.
- Criteria about scripts, history and test format are tagged `[process]` and are not counted for API coverage.

## 2026-10-04: Roles and permissions implemented (Task 1)

- Retired REQ-010.AC4 and REQ-014.AC1/AC2 (old scope model), replaced by REQ-010.AC6 and REQ-014.AC3. Their baseline
  cases ACC-005..007 were deleted. The baseline fixture now logs in as the bootstrap admin instead of setting scope
  ANY in SQL.
- New criterion tag `[retired]`.


## 2026-10-15: Route claims in the token (branch `self-service`)

| # | What was asked or decided | Source | Requirements |
|---|---|---|---|
| 1 | The backend APIs need role-based auth. Teachers can see all data but can't edit anything; no course-level restriction for now. | User | REQ-011, REQ-012 |
| 2 | Don't overcomplicate: JWTs carry a list of routes, with read and write for each path separately. No built-in TEACHER role. | User | REQ-011.AC6-AC9, REQ-012.AC7-AC11, REQ-016 |

- Retired REQ-010.AC5, REQ-011.AC1/AC2/AC4/AC5, REQ-012.AC1/AC2/AC3/AC5/AC6 and REQ-013.AC3 (per-resource
  permissions loaded on every request). `read` no longer comes with `write`, and role changes now apply at the next
  login or refresh instead of the next request.
- `/api/discounts/**` is its own route instead of sharing PAYMENT. `/api/fee-timeline/**` is not in the catalogue
  because that feature is not on this branch.

## 2026-10-15: Postman replaces the YAML contract (branch `self-service`)

| # | What was asked or decided | Source | Requirements |
|---|---|---|---|
| 1 | The testing setup is Postman-based: hit the APIs and verify the responses, nothing more. Cover all existing features, and keep expanding the cases as features are added. | User | REQ-002, REQ-003.AC1 |
| 2 | Postman replaces the YAML suite in `contract/`; run with Newman from the command line and openable in the Postman app; a fresh database per run. | User (options chosen) | REQ-002.AC2, REQ-002.AC3 |

- All 112 cases that were green in the YAML suite were ported, keeping their IDs and `Proves:` citations. Not ported:
  PLT-T01 and PLT-T02 (still red, REQ-005.AC1/AC2), PLT-T05 (a SQL check; a failed migration already stops the
  runner), and the fee-timeline migration specs (that feature is not on this branch).

## 2026-10-15: Route-based auth tested on every endpoint

| # | What was asked or decided | Source | Requirements |
|---|---|---|---|
| 1 | Make test cases that check the route-based auth works for each of the APIs. | User | REQ-011.AC10, REQ-011.AC11 |

## 2026-10-15: Logs in the database

| # | What was asked or decided | Source | Requirements |
|---|---|---|---|
| 1 | Store raw logs in the database in a new `log` table with session id, request id, body and user (from the JWT); the request context must be available anywhere in the code. | User | REQ-008 |
| 2 | Log every request/response, the application's log statements and errors. Session id comes from a client header (`X-Session-Id`). Bodies are stored with secrets masked. Storage is an interface with a database implementation only; no read API or retention yet. | User (options chosen) | REQ-008.AC2-AC6 |

- New criterion tag `[manual]` for behaviour the API can't show (rows in `log`).
