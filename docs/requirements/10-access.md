# Access control

Access is granted per **route**: a path pattern from a fixed catalogue, with separate `read` and `write` flags.
The catalogue:

| Path | Covers |
|---|---|
| `/api/students/**` | students |
| `/api/student-sessions/**` | enrolments |
| `/api/courses/**` | courses |
| `/api/fee-structures/**` | fee structures |
| `/api/payments/**` | payments |
| `/api/discounts/**` | discounts |
| `/api/dues/**` | dues |
| `/api/users/**` | user administration |
| `/api/roles/**` | role administration |
| `/api/logging/**` | logging settings |

Always open: `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/users/signup`, `/heartbeat`, `OPTIONS`.
`GET /api/users/me` needs only a valid access token.

### REQ-010 Login and token refresh
Status: implemented · Source: existing code, 2026-10-04

- **REQ-010.AC1** [baseline] `POST /api/auth/login` `{email, password}` with valid credentials of an administrator →
  200 `{accessToken, refreshToken}`.
- **REQ-010.AC2** [baseline] `POST /api/auth/refresh` with `Authorization: Bearer <refreshToken>` → 200 with a new
  `accessToken` and the same `refreshToken`.
- **REQ-010.AC3** [baseline] A request to a protected endpoint with a valid access token of an administrator is
  allowed.
- **REQ-010.AC4** [retired] *(was baseline-defect)* A wrong password returned HTTP 400 (with `"status": 403` inside
  the body). Replaced by REQ-010.AC6.
- **REQ-010.AC5** [retired] *(was target)* Login returned `permissions` such as `["PAYMENT_READ"]`. Replaced by
  REQ-010.AC9.
- **REQ-010.AC6** [target] An unknown email or a wrong password → 401 with the same message for both, so the API
  doesn't reveal which emails exist.
- **REQ-010.AC7** [target] A protected endpoint without a token, or with an expired or invalid token → 401.
- **REQ-010.AC8** [target] `POST /api/auth/refresh` with an access token instead of a refresh token, or with an
  invalid token → 401.
- **REQ-010.AC9** [target] `POST /api/auth/login` and `POST /api/auth/refresh` with valid credentials → 200
  `{accessToken, refreshToken, routes}`. `routes` is the user's effective routes, the same list the access token
  carries (REQ-016).

### REQ-011 Per-route read and write access
Status: implemented · Source: user, 2026-10-04; changed 2026-10-15

- **REQ-011.AC1** [retired] *(was target)* `GET` needed `<RESOURCE>_READ`, other methods `<RESOURCE>_WRITE`.
  Replaced by REQ-011.AC6.
- **REQ-011.AC2** [retired] *(was target)* `WRITE` implied `READ`. Replaced by REQ-011.AC7.
- **REQ-011.AC3** [target] A user without the required flag → 403 with the standard error body, and nothing
  changes.
- **REQ-011.AC4** [retired] *(was target)* Per-resource READ/none checks. Replaced by REQ-011.AC8.
- **REQ-011.AC5** [retired] *(was target)* Permission changes applied on the next request. Replaced by
  REQ-011.AC9.
- **REQ-011.AC6** [target] `GET` and `HEAD` on a catalogue path need `read` on that route. `POST`, `PUT`, `PATCH` and
  `DELETE` need `write`.
- **REQ-011.AC7** [target] `read` and `write` are independent: a route with only `write` allows writes but a `GET`
  on it → 403.
- **REQ-011.AC8** [target] For every catalogue route, a user with only `read` can `GET` but gets 403 on a write. A
  user with neither flag gets 403 on both.
- **REQ-011.AC9** [target] Access is decided from the token alone. A role change applies when the user next logs in
  or refreshes; until then their current access token keeps its old routes (at most 30 minutes).

### REQ-012 Named roles
Status: implemented · Source: user, 2026-10-04; changed 2026-10-15

Roles are named lists of routes. Users get one or more roles. A role such as "Teacher" (read on every route, write on
none) is created by an admin; only `ADMIN` is built in.

- **REQ-012.AC1** [retired] *(was target)* Roles took `permissions: {RESOURCE: READ|WRITE|NONE}`. Replaced by
  REQ-012.AC7.
- **REQ-012.AC2** [retired] *(was target)* Roles were returned with `permissions: {RESOURCE: access}`. Replaced by
  REQ-012.AC8.
- **REQ-012.AC3** [retired] *(was target)* Role changes affected holders immediately. Replaced by REQ-012.AC9.
- **REQ-012.AC4** [target] `DELETE /api/roles/{id}` → 204 and removes the role from all users. Deleting the built-in
  `ADMIN` role → 409.
- **REQ-012.AC5** [retired] *(was target)* Union of resource levels. Replaced by REQ-012.AC10.
- **REQ-012.AC6** [retired] *(was target)* ADMIN had WRITE on every resource. Replaced by REQ-012.AC11.
- **REQ-012.AC7** [target] `POST /api/roles` `{name, description, routes: [{path, read, write}]}` → 201. A duplicate
  name → 409. A path outside the catalogue, the same path twice, or a blank name → 400.
- **REQ-012.AC8** [target] `GET /api/roles` (optionally `?name=`, exact match) and `GET /api/roles/{id}` return roles
  as `{id, name, description, builtIn, routes: [{path, read, write}]}`. Every catalogue path is listed, in catalogue
  order, with `false` flags where nothing is granted. An unknown id → 404.
- **REQ-012.AC9** [target] `PUT /api/roles/{id}` replaces the name, description and routes. Holders get the new routes
  at their next login or refresh (REQ-011.AC9).
- **REQ-012.AC10** [target] A user holding several roles gets, per path, `read` if any role grants read and `write` if
  any role grants write.
- **REQ-012.AC11** [target] A built-in `ADMIN` role exists with `read` and `write` on every catalogue path.

### REQ-013 User administration
Status: implemented · Source: user, 2026-10-04

- **REQ-013.AC1** [target] `GET /api/users` lists users `{id, email, roles}`. Password hashes are never returned.
- **REQ-013.AC2** [target] `PUT /api/users/{id}/roles` `{roleIds: [..]}` replaces the user's roles. An unknown role
  ID → 400. An unknown user → 404.
- **REQ-013.AC3** [retired] *(was target)* `/me` returned `permissions`. Replaced by REQ-013.AC5.
- **REQ-013.AC4** [target] An admin cannot remove `ADMIN` from themselves if they are the last admin → 409.
- **REQ-013.AC5** [target] `GET /api/users/me` returns the caller's `{id, email, roles, routes}`, read from the
  database (so it shows role changes before the token does). Any authenticated user may call it, whatever their
  routes.

### REQ-014 Self sign-up gives no access
Status: implemented · Source: existing code, 2026-10-04

- **REQ-014.AC1** [retired] *(was baseline)* `POST /api/users/signup` returned `{id, email, scope: "NONE"}`.
  Replaced by REQ-014.AC3.
- **REQ-014.AC2** [retired] *(was baseline)* A signed-up user with scope `NONE` could not log in. Replaced by
  REQ-014.AC3: they can log in but have no permissions.
- **REQ-014.AC3** [target] `POST /api/users/signup` → 201 `{id, email, roles: []}`. The user can log in (with
  `routes: []`) but gets 403 on every catalogue route until an admin assigns a role.
- **REQ-014.AC4** [target] Signing up with an email that already exists → 409. A missing email or password → 400.

### REQ-015 Bootstrap administrator
Status: implemented (proposed) · Source: design, 2026-10-04

A fresh database (tests, new environments) needs a first admin without editing SQL by hand.

- **REQ-015.AC1** [target] If properties `sonata.bootstrap-admin.email` and `.password` are set and no account with
  that email exists, it is created at startup with the `ADMIN` role. An existing account with that email is left
  unchanged. Without the properties nothing happens.

### REQ-016 Routes travel in the access token
Status: implemented · Source: user, 2026-10-15

- **REQ-016.AC1** [target] The access token's payload is
  `{sub: <email>, type: "ACCESS", iat, exp, routes: [{path, read, write}]}`, listing only routes with at least one
  flag set. It expires after 30 minutes.
- **REQ-016.AC2** [target] The refresh token's payload is `{sub, type: "REFRESH", iat, exp}`, with no routes. It
  expires after 7 days. Refresh re-reads the user's roles from the database.
