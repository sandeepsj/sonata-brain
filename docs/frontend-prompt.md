# Prompt for the frontend (sonata-face)

Paste the block below into a Claude Code session opened in `~/dev/sonata-face`.

One decision is built in. Some frontend screens use endpoints this backend branch doesn't have: fee timeline, dues
rebuild, waivers, course close/reopen, payment conversion and fee-structure clone. The prompt tells the agent to
**disable those screens, not delete them or rewire them to the old endpoints**, until it's decided whether those
features come back. Change step 7 if that has been decided.

```text
The backend (repo ~/dev/sonata-brain, branch `self-service`) changed its auth model and added request logging.
Update this SvelteKit frontend to match. The backend's generated OpenAPI spec is the contract; when it disagrees
with anything else (including this prompt), the spec wins.

## Read first (backend repo, read-only — don't edit it)
1. ~/dev/sonata-brain/docs/frontend-handoff.md — what changed, what to do, and a table of which of this app's
   API calls exist on the backend branch. Start here.
2. ~/dev/sonata-brain/docs/api/openapi.json — the exact contract (paths, params, bodies, responses, auth, which
   route each operation needs).
3. ~/dev/sonata-brain/docs/requirements/10-access.md — login, routes, roles, users (REQ-010..REQ-016).
4. ~/dev/sonata-brain/docs/requirements/00-platform.md — error body (REQ-005), logging headers (REQ-008),
   API docs (REQ-009).
5. Real request/response examples: ~/dev/sonata-brain/postman/collections/access.postman_collection.json and
   route-access.postman_collection.json (open as JSON; each request's Tests show the expected response).

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
   regenerating.
2. Routes replace permissions. Login, refresh and GET /api/users/me return
   `routes: [{path, read, write}]` instead of `permissions: string[]`. Replace every permission check with a
   helper like `can(routes, '/api/payments/**', 'read' | 'write')`. A screen shows when its route has `read`;
   create/edit/delete actions show when it has `write`. Read and write are independent. Update the landing-page
   logic and the no-access page (sign-up gives `routes: []`).
3. Roles admin screen: the role body is now `{name, description, routes: [{path, read, write}]}`; GET returns
   all 10 catalogue routes with false flags where not granted. Edit it as a table: one row per route path, two
   checkboxes (Read, Write). Show 400 errors on the fields. Built-in ADMIN can't be deleted (409).
4. Tokens: on 401, call POST /api/auth/refresh once with `Authorization: Bearer <refreshToken>`, retry, then go
   to login. On 403 show "You don't have permission". Role changes reach the token only at the next refresh, so
   keep re-reading /api/users/me (on navigation and periodically) and refresh the token when its routes differ.
5. Logging headers: send `X-Session-Id` on every request (one id per tab, stored in sessionStorage). Read the
   `X-Request-Id` response header and show it in error toasts as a reference ("ref 3f2a…").
6. Errors: show `message` from `{status, error, message, details[], timestamp}` and map `details[].field` onto
   form fields. Some older endpoints return an empty body on 400/404 — fall back to a generic message. Payment
   create/deactivate errors use `{error, message, status, timestamp}` without details.
7. Screens whose endpoints don't exist on the backend branch (see the table at the end of
   frontend-handoff.md: fee timeline, dues rebuild/schedule/legacy-comparison, waivers, course close/reopen,
   payment convert-to-course-fee, fee-structure list/clone/edit/delete, the query-param list endpoints for
   sessions, payments and dues): do NOT delete them and do NOT rewire them to legacy endpoints. Hide them from
   navigation behind one feature flag and leave the code intact. List them in your final summary.

## How to work
- First, list every API call the app makes and map each one to its operation in openapi.json, or mark it
  "not on backend". Show me that list before changing code.
- After each step, check it against the running backend in the browser: log in as admin; create a "Teacher"
  role (read on students, student-sessions, courses, fee-structures, payments, discounts, dues; write on none);
  sign up a user, assign the role, log in as them and confirm write actions are hidden and that a forced write
  gives the 403 message; confirm requests carry X-Session-Id and error toasts show the request id.
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
