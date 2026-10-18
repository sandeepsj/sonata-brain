# Prompt for the frontend (sonata-face)

Paste the block below into a Claude Code session opened in `~/dev/sonata-face`.

One decision is built in. Screens whose endpoints this backend branch doesn't have (course close/reopen, dues aggregate,
waivers, legacy comparison) are **disabled, not deleted or rewired**. Change step 8 if
that has been decided.

```text
The backend (repo ~/dev/sonata-brain, branch `main`) changed its auth model, added request logging, and replaced the
fee model: school-wide fee structures on a timeline that changes only at term boundaries, with dues calculated from
enrolments and payments. Production already runs this backend, so the current frontend is broken until this is done.
Update this SvelteKit frontend to match. The backend's generated OpenAPI spec is the contract; when it disagrees
with anything else (including this prompt), the spec wins.

## Read first (backend repo, read-only — don't edit it)
1. ~/dev/sonata-brain/docs/frontend-handoff.md — what changed, what to do, and a table of which of this app's
   API calls exist on the backend branch. Start here.
2. ~/dev/sonata-brain/docs/api/openapi.json — the exact contract (paths, params, bodies, responses, auth, which
   route each operation needs).
3. ~/dev/sonata-brain/docs/requirements/10-access.md — login, routes, roles, users (REQ-010..REQ-016);
   50-fees.md, 60-payments.md and 70-dues.md — fee structures, the timeline, payments and dues.
4. ~/dev/sonata-brain/docs/requirements/00-platform.md — error body (REQ-005), logging headers (REQ-008),
   API docs (REQ-009).
5. Real request/response examples in ~/dev/sonata-brain/postman/collections/ (open as JSON; each request's Tests
   show the expected response): access, payments, fee-structures, fee-timeline and dues collections.

In this repo, read before changing anything: src/lib/api/client.ts, src/lib/api/types.ts, the auth store in
src/lib/stores/, src/routes/+layout.svelte, src/routes/(app)/+layout.svelte, src/routes/(app)/no-access,
src/routes/login, src/routes/(app)/admin/roles, and every place that reads `permissions`.

## Running the backend locally
cd ~/dev/sonata-brain/postman && npm ci && node run.js --keep-server RTE-open
→ backend on http://localhost:18080 with a fresh database, admin root@sonata.test / root-password-1.
Browsable docs: http://localhost:18080/swagger-ui.html. CORS only allows https://sonata-face.vercel.app, so in
dev add a Vite proxy (`/api` → http://localhost:18080) rather than calling the backend cross-origin.

## What to do
1. Generate API types from the spec instead of hand-writing them:
   npx openapi-typescript ~/dev/sonata-brain/docs/api/openapi.json -o src/lib/api/schema.d.ts
   Use them in client.ts/types.ts. Keep all API calls in the one client module. Add an npm script for
   regenerating. Ignore the `regularPayment` and `validZeroPayment` properties the spec shows on
   PaymentCreateRequest (internal helpers; never send them).
2. Routes replace permissions. Login, refresh and GET /api/users/me return
   `routes: [{path, read, write}]` instead of `permissions: string[]`. Replace every permission check with a
   helper like `can(routes, '/api/payments/**', 'read' | 'write')`. A screen shows when its route has `read`;
   create/edit/delete actions show when it has `write`. Read and write are independent. Update the landing-page
   logic and the no-access page (sign-up gives `routes: []`).
3. Roles admin screen: the role body is now `{name, description, routes: [{path, read, write}]}`; GET returns
   all 12 catalogue routes with false flags where not granted. Edit it as a table: one row per route path, two
   checkboxes (Read, Write). Show 400 errors on the fields. Built-in ADMIN can't be deleted (409).
4. Tokens: on 401, call POST /api/auth/refresh once with `Authorization: Bearer <refreshToken>`, retry, then go
   to login. On 403 show "You don't have permission". Role changes reach the token only at the next refresh, so
   keep re-reading /api/users/me (on navigation and periodically) and refresh the token when its routes differ.
5. Logging headers: send `X-Session-Id` on every request (one id per tab, stored in sessionStorage). Read the
   `X-Request-Id` response header and show it in error toasts as a reference ("ref 3f2a…").
6. Errors: show `message` from `{status, error, message, details[], timestamp}` and map `details[].field` onto
   form fields. Some older endpoints return an empty body on 400/404 — fall back to a generic message. Payment
   create/deactivate errors use `{error, message, status, timestamp}` without details.
7. Fees, timeline, dues and payments (section 4 of frontend-handoff.md):
   - Fee structures: one form with a name, a term length in months and an amount per course (one row per course).
     No course selector and no per-term amounts. Delete is disabled while `usedInTimeline`.
   - Timeline: one school-wide list of `{structure, from}` entries. When adding an entry, offer only valid term
     starts from `GET /api/fee-timeline/preview` and show the backend's 400 message as is. Show the preview next to
     the editor.
   - Fee changes recalculate everyone's dues, so confirm them twice: for timeline add/change/delete and for editing
     a structure with `usedInTimeline: true`, first send the request with `?dryRun=true` and show its
     `recalculation` impact (students affected, outstanding before → after); enable "Apply" only after an explicit
     confirmation (checkbox or typing the start date); then send it for real, show a blocking progress state while
     it runs, and show the returned `recalculation` as the result. Timeline delete answers 200 with
     `{dryRun, recalculation}`.
   - Dues: show periods as dates (`periodStart` – `periodEnd`), status DUE / PARTIALLY_PAID / PAID, and the summary's
     `credit`. Add an admin "Recalculate dues" action using the rebuild job.
   - Payments: remove the term/period picker and every `termId` / `periodDate`; send `paymentType`
     (REGULAR | ADDITIONAL), `courseName` (required for REGULAR) and a required `label`. Explain that a course fee
     pays the oldest unpaid term first. "Forgive a term" = a ₹0 REGULAR payment with a discount.
   - "Convert to course fee" action on ADDITIONAL payments: `POST /api/payments/{id}/convert-to-course-fee`
     `{ courseName?, amount? }`. Add an editable "amount to convert" field, defaulting to the smaller of the payment
     and the course's open dues; when it's less than the payment, explain that the rest stays an additional payment
     (the response is a new REGULAR payment with `convertedFrom`). Refresh the student's dues and payments afterwards.
8. Courses: show `status`; offer Close / Reopen (`PATCH /api/courses/{name}/close|reopen`) and a status filter
   (`?status=ACTIVE|CLOSED`); leave closed courses out of enrolment and course-fee pickers (the backend answers 409).
   Sessions: "End on a past date" sends `PATCH /api/student-sessions/{id}/disjoin` with `{ "date": "YYYY-MM-DD" }`;
   use `GET /api/student-sessions?studentId=&courseName=&active=&activeOn=` for lists and the roster "as of a date".
   Payments: use `GET /api/payments?studentId=&courseName=&type=&active=&from=&to=` (newest first) for the payments
   page.
9. Screens whose endpoints don't exist on the backend (dues aggregate reports, waivers, legacy comparison): do NOT
   delete them and do NOT rewire them to other endpoints. Hide them from navigation behind one feature flag and
   leave the code intact. List them in your final summary.

## How to work
- First, list every API call the app makes and map each one to its operation in openapi.json, or mark it
  "not on backend". Show me that list before changing code.
- After each step, check it against the running backend in the browser: log in as admin; create a "Teacher"
  role (read on students, student-sessions, courses, fee-structures, fee-timeline, payments, discounts, dues;
  write on none);
  sign up a user, assign the role, log in as them and confirm write actions are hidden and that a forced write
  gives the 403 message; confirm requests carry X-Session-Id and error toasts show the request id.
- Test timeline and fee-structure edits only against the local backend: on production a full dues recalculation
  is still slow (being fixed) and those requests would time out.
- Keep the existing look and component patterns. Run the project's type check and build before finishing.
```

## Files the frontend agent reads (backend repo)

| File | Why |
|---|---|
| `docs/frontend-handoff.md` | The brief: what changed, what to do, and the gap table |
| `docs/api/openapi.json` | The exact contract; types are generated from it |
| `docs/requirements/10-access.md` | Rules for login, routes, roles and users |
| `docs/requirements/00-platform.md` | Error body (REQ-005), logging headers (REQ-008), API docs (REQ-009) |
| `postman/collections/access.postman_collection.json`, `route-access.postman_collection.json` | Real request and response examples |

## Prompt: receipt templates (2026-10-18)

Paste this on its own once the main migration above is done.

```text
The backend (repo ~/dev/sonata-brain, branch `main`) now stores receipt templates. A template is a complete HTML
document (CSS and <script> allowed) with Handlebars variables; this frontend fills in the variables and renders it,
and the rendered page is the receipt. This replaces the per-element formatting plan in
docs/receipt-templates-requirement.md (mark that file superseded) and the hard-coded layout in
src/routes/receipt-print/+page.svelte.

Read first (backend repo, read-only): ~/dev/sonata-brain/docs/frontend-handoff.md section 5,
~/dev/sonata-brain/docs/requirements/90-receipts.md, the /api/receipt-templates operations in
~/dev/sonata-brain/docs/api/openapi.json, and the examples in
~/dev/sonata-brain/postman/collections/receipt-templates.postman_collection.json.
Regenerate the API types from openapi.json.

What to do:
1. Add `/api/receipt-templates/**` ("Receipt templates") to the route list and the roles editor (12 routes now).
2. A receipt data builder: from the student and the selected payments, build the variables object exactly as listed
   in 90-receipts.md (amounts formatted en-IN without ₹, dates like "18 Oct 2026"). Render with the `handlebars`
   package, and also expose the object to scripts as `window.receipt`.
3. Render and print ONLY inside `<iframe sandbox="allow-scripts allow-modals" srcdoc=…>`, never with
   allow-same-origin, so a template's script can't reach the token or the API. Print by injecting a
   `window.print()` call into the iframe document.
4. Printing (students/[id]/receipts and /receipt-print): a template picker (GET /api/receipt-templates, default
   preselected via GET /api/receipt-templates/default), then render the chosen template in the iframe and print.
   The output with the Standard template must look like today's receipt.
5. A templates editor page: list, HTML code editor (CodeMirror 6, HTML mode), a variables panel (click inserts
   {{…}} at the cursor; show {{#each payments}}…{{/each}} as a snippet), and a live preview in the sandboxed iframe
   with sample data. Save (PUT), Save as new… (POST, asks for a name), Make default (PATCH …/default), Delete with
   confirmation. Show write actions only with write on the route; the built-in template offers only "Save as new".
   Show 400 `details` on the name/html fields and 409 messages as is.

Check everything against a local backend (cd ~/dev/sonata-brain/postman && node run.js --keep-server RTE-open):
print with Standard, create a template with a script and confirm it runs in the preview but can't read
localStorage, make it default, delete it. Run the type check and build before finishing.
```
