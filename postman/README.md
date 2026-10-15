# API tests (Postman)

Black-box tests for the Sonata backend. They call the HTTP API and check the responses, nothing more. There is
one Postman collection per feature in `collections/`, and the same files run from the command line (Newman) and in
the Postman app.

## Running

Needs Node 18+, Java 17 and a local PostgreSQL whose user can create databases.

```bash
cd postman
npm ci                         # once
npm test                       # build, fresh database, run every case (same as: node run.js)
node run.js Payments           # one collection
node run.js STU-01 PAY-004     # cases whose name starts with these
node run.js --no-build         # skip the Maven build
node run.js --keep-server      # leave the backend on http://localhost:18080 for the Postman app
node run.js --base-url http://localhost:8080   # a backend you started yourself (no build, no data reset)
npm run trace                  # which requirement criteria have no case yet
```

What a run does:
1. Builds the jar and creates a fresh database `sonata-postman`. Override it with `TEST_DB`; the connection
   comes from `PGHOST`, `PGPORT`, `PGUSER` and `PGPASSWORD`, defaulting to localhost, sandeepsj and sonata123.
2. Starts the backend on port 18080 with the bootstrap admin `root@sonata.test`. The log goes to `.out/server.log`.
3. For **each case**, empties every table except `flyway_schema_history`, `account`, `role`, `role_route` and
   `user_roles`. It then runs the collection's `00 Setup` folder (admin login), its `00 Before each` folder if
   there is one, and the case folder. Cases never see each other's data.

## In the Postman app

Import every file in `collections/` plus `local.postman_environment.json`, and select the "Sonata local"
environment. Start a backend with `node run.js --keep-server`, or point `baseUrl` at your own. Run `00 Setup`
first (and `00 Before each` if the collection has one), then any case folder. Some cases count records, for
example "12 students give 2 pages", so they only pass on an empty database. `run.js` takes care of that.

## Writing a case

- **Folder:** a case is a folder in its feature's collection named `<ID> <title>`, for example
  `STU-007 Every provided field is stored and returned`. The IDs are stable, so never reuse one. The folder
  description lists the acceptance criteria it proves: `Proves: REQ-020.AC6`.
- **Requests:** the requests inside a case run in order. Name them `<ID> <nn> <METHOD> <path>` so names are
  unique; retries depend on that.
- **Auth:** requests inherit `Bearer {{admin}}` from the collection. To act as another user, set the request's
  auth to Bearer `{{thatToken}}`. Use "No Auth" for anonymous calls.
- **Checks:** put them in each request's Tests tab, using plain `pm.test` and `pm.expect`:
  ```js
  pm.test("status", () => pm.response.to.have.status(201));
  pm.test("body", () => {
    const b = pm.response.json();
    pm.expect(b.studentName).to.eql("Kiran");
    pm.expect(b.routes).to.have.lengthOf(2);
  });
  pm.collectionVariables.set("sid", pm.response.json().id);   // for later requests: {{sid}}
  ```
- **Typed values:** a saved number or list can't be written as `{{x}}` inside raw JSON, so build such bodies in the
  Pre-request tab:
  `pm.request.body.raw = JSON.stringify({ roleIds: [pm.collectionVariables.get("rid")] })`.
- **Unique names:** use `u{{$randomUUID}}` in emails and role names. Users and roles are not emptied between
  cases.
- **Dates:** the collection sets `{{today}}`, `{{today_m40d}}` (40 days ago), `{{year}}` and `{{year_m1}}` before
  every request. Add new offsets to the collection's Pre-request script.
- **Asynchronous results** (dues are updated after an event): retry the request until its checks pass. Copy the
  pattern from any `DUE-L` case: `pm.execution.setNextRequest(pm.info.requestName)`, up to 30 times.
- **Error bodies:** where a case expects the standard error body, it checks `status`, `error`, `message`,
  `details` and `timestamp`.

For a new feature, add the requirement to `docs/requirements` first, then the cases, then the code. `npm test`
must be green before every commit.
