# Frontend handoff (backend branch `dues-timeline`)

## The contract
- **`docs/api/openapi.json`** is the source of truth: an OpenAPI 3 spec generated from the backend code. It is
  committed and rewritten on every full test run, so API changes show up as diffs in backend commits.
- **Browsable docs:** start the backend and open `http://localhost:8080/swagger-ui.html`. Click **Authorize**, paste
  an access token, and try calls.
- **Raw spec of a running backend:** `GET /v3/api-docs`. No token needed.
- **TypeScript types:** generate them instead of hand-writing them:
  ```bash
  npx openapi-typescript ../sonata-brain/docs/api/openapi.json -o src/lib/api/schema.d.ts
  ```
- **Examples:** every behaviour is also pinned down by real request/response examples in
  `postman/collections/*.json`, which you can open in the Postman app.
- **Requirements:** `docs/requirements/*.md` explains the *why* (IDs like REQ-011).

## What changed and what the frontend must do

### 1. Routes replace `permissions`
`POST /api/auth/login`, `POST /api/auth/refresh` and `GET /api/users/me` now return
`routes: [{ path, read, write }]` instead of `permissions: ["PAYMENT_READ", ...]`. They list only the routes
the user has some access to:
```json
{ "accessToken": "…", "refreshToken": "…",
  "routes": [ { "path": "/api/students/**", "read": true, "write": false },
              { "path": "/api/payments/**", "read": true, "write": true } ] }
```
- `GET` (and `HEAD`) on a path needs `read`; `POST`, `PUT`, `PATCH` and `DELETE` need `write`. The two are independent.
- **Show a screen** when its route has `read`. **Show create/edit/delete buttons** when it has `write`.
- **Route list:** there are 11 paths in total: `/api/students/**`, `/api/student-sessions/**`, `/api/courses/**`,
  `/api/fee-structures/**`, `/api/fee-timeline/**`, `/api/payments/**`, `/api/discounts/**`, `/api/dues/**`,
  `/api/users/**`, `/api/roles/**`, `/api/logging/**`. Each operation in the spec says which route it needs.
- **A "Teacher":** this is just a role that has `read` on the data routes and `write` on none.
- **Role-editor screen:** the role body is now `{ name, description, routes: [{ path, read, write }] }`.
  `GET /api/roles` returns every route, with `false` flags where nothing is granted. Unknown or repeated paths → 400.
- **Sign-up:** a new account has `routes: []`. Show the "no access, ask an admin" page.

### 2. Tokens and errors
- **Sending the token:** `Authorization: Bearer <accessToken>`. The access token lasts 30 minutes; the refresh
  token lasts 7 days.
- **On 401:** call `POST /api/auth/refresh` once with `Authorization: Bearer <refreshToken>`, retry the request,
  and go to login if it fails again.
- **On 403:** show "You don't have permission".
- **Role changes** reach the token only at the next refresh, up to 30 minutes later. `GET /api/users/me` reads the
  database and shows them at once. Re-read it on navigation (or every minute) to update the menu, and refresh the
  token when the routes differ from the ones you have.
- **Error body:** `{ status, error, message, details: [{ field, message }], timestamp }`. Show `message` and put
  `details[].field` errors on the form fields.
  - Some older endpoints still answer 404 or 400 with an **empty body**, so fall back to a generic message.
  - `POST /api/payments` and `PATCH /api/payments/{id}/deactivate` use their own error map:
    `{ error, message, status, timestamp }`, with no `details`.

### 3. Request and session ids (logging)
- **Session id:** create one id per browser tab and send it on every request:
  ```ts
  const sessionId = sessionStorage.getItem('sid') ?? crypto.randomUUID();
  sessionStorage.setItem('sid', sessionId);
  headers['X-Session-Id'] = sessionId;
  ```
- **Request id:** every response has an `X-Request-Id` header. Show it in error toasts ("ref 3f2a…") so a problem
  can be found in the backend's `log` table. You may also send your own `X-Request-Id` (1–64 letters, digits or
  `-`); it is echoed back.
- **CORS:** both headers are allowed, and `X-Request-Id` is exposed to the browser. The only allowed origin is
  currently `https://sonata-face.vercel.app`. A local dev frontend (`localhost:5173`) needs a dev proxy, or that
  origin added to the backend's CORS config.

### 4. Fees, the timeline and dues
This branch replaces the per-year, per-term fee rows with three ideas. Read `docs/requirements/50-fees.md` and
`70-dues.md` for the exact rules.

- **Fee structure:** `{ id, name, termMonths, amounts: [{ courseName, amount }], usedInTimeline }`. One structure
  covers every subject; a subject left out isn't billed while that structure applies.
  - CRUD at `/api/fee-structures`. Delete gives 409 while the structure is on the timeline.
  - Editing a structure that is on the timeline is allowed, and recalculates everyone's dues. Changing `termMonths`
    gives 409 when it would break the timeline.
- **Timeline:** one for the whole school. Each entry is `{ id, feeStructureId, feeStructureName, termMonths,
  effectiveFrom, effectiveTo }` and runs until the next entry starts.
  - `GET/POST/PUT/DELETE /api/fee-timeline`.
  - **A new structure may start only at the beginning of a term** of the one before it. Otherwise → 400, and the
    message names the nearest valid dates, so show it as is. Offer a date picker of valid term starts: use the preview.
  - `GET /api/fee-timeline/preview?from=&to=` lists the terms `[{ periodStart, periodEnd, feeStructureName,
    amounts }]` without saving anything. Show it next to the editor.
  - Every change recalculates all dues before the response returns (a few seconds at most).
- **Dues** are calculated, never edited:
  - `GET /api/dues?studentId=&courseName=&status=&from=&to=` returns `{ studentId, studentName, courseName,
    periodStart, periodEnd, feeStructureName, amount, paidAmount, discountAmount, balance, status }`, where status
    is `DUE`, `PARTIALLY_PAID` or `PAID`. Show periods as dates; there are no term numbers.
  - `GET /api/dues/summary?studentId=` returns per course and in total the `amount`, `paid`, `discount`, `balance`
    and `credit`. Credit is money paid beyond what is due, and it pays new terms as they start.
  - Admin: `POST /api/dues/rebuild?scope=ALL|STUDENT&id=` → 202 `{ jobId }`; poll `GET /api/dues/rebuild/{jobId}`.
- **Payments** have no term:
  - `POST /api/payments` takes `{ studentId, paymentType: "REGULAR"|"ADDITIONAL", courseName, amountPaid, label,
    date?, remarks?, discounts? }`. `paymentType` and `label` are required; REGULAR needs `courseName` and a session
    in that course.
  - A REGULAR payment pays the student's **oldest unpaid term** of that course first, then the next. There's no
    period picker and no upper limit.
  - To **forgive** a term (for example, a student left early), record a ₹0 REGULAR payment with a discount labelled
    with the reason. Dues are recalculated straight away.

## Known gaps in the spec
- **Untyped success responses.** These say "object" because the controllers return untyped maps:
  - `POST /api/payments`: really 201 with the payment
  - `PATCH /api/payments/{id}/deactivate`

  The Postman payments cases show their real shape.
- **Create status codes.** The spec lists 200 for creates that answer 200 today (students, courses, sessions).
  Fee structures, timeline entries, sign-up and roles answer 201.

## ⚠ sonata-face currently targets a different API
`sonata-face` was written against the old rewrite branch (`design/fee-timeline-and-permissions`), which had
per-course structures. This branch has school-wide ones, so the fee screens need changing. The frontend's calls today:

| Frontend calls | On `dues-timeline` |
|---|---|
| `POST /api/auth/login`, `/api/auth/refresh`, `GET /api/users/me` | Exist; `permissions` became `routes` (section 1) |
| `GET/POST/PUT/DELETE /api/roles`, `GET /api/users`, `PUT /api/users/{id}/roles`, `POST /api/users/signup` | Exist; role body uses `routes` |
| `GET/POST /api/students`, `GET /api/students/search` | Exist |
| `GET/POST /api/courses` | Exist. `PATCH /api/courses/{name}/close` and `/reopen` and `?status=` **don't** |
| `POST /api/student-sessions`, `PATCH /api/student-sessions/{id}/disjoin` | Exist. The list `GET /api/student-sessions?studentId=&courseName=…` **doesn't**; use `/all`, `/search?course=`, `/active/*` |
| `POST /api/payments`, `PATCH /api/payments/{id}/deactivate`, `GET /api/payments` | Exist with `paymentType` and **no** `periodDate` or `termId` (section 4). `convert-to-course-fee` and the filter params **don't** |
| `/api/fee-structures` | Exist in the **new school-wide shape** (section 4). No `courseName`, `termsPerYear` or clone |
| `/api/fee-timeline`, `/api/fee-timeline/preview` | Exist, **school-wide** (no `courseName`), boundary-only (section 4) |
| `/api/dues?…`, `/api/dues/summary?studentId=`, `/api/dues/rebuild*` | Exist (section 4). `/api/dues/aggregate`, `/api/dues/waivers` and `/api/dues/legacy-comparison` **don't** (waivers were replaced by ₹0 discount payments) |
