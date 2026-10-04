# Access control

Resources: `STUDENT`, `SESSION`, `PAYMENT`, `COURSE`, `FEE_STRUCTURE`, `DUES`, `USER`.
Path mapping: `/api/students/**`→STUDENT, `/api/student-sessions/**`→SESSION, `/api/payments/**` and
`/api/discounts/**`→PAYMENT, `/api/courses/**`→COURSE, `/api/fee-structures/**` and `/api/fee-timeline/**`→FEE_STRUCTURE,
`/api/dues/**`→DUES, `/api/users/**`, `/api/roles/**` and `/api/logging/**`→USER.

### REQ-010 Login and token refresh
Status: verified · Source: existing code, 2026-10-04

- **REQ-010.AC1** [baseline] `POST /api/auth/login` `{email, password}` with valid credentials of an administrator →
  200 `{accessToken, refreshToken}`.
- **REQ-010.AC2** [baseline] `POST /api/auth/refresh` with `Authorization: Bearer <refreshToken>` → 200 with a new
  `accessToken` and the same `refreshToken`.
- **REQ-010.AC3** [baseline] A request to a protected endpoint with a valid access token of an administrator is
  allowed.
- **REQ-010.AC4** [retired] *(was baseline-defect)* A wrong password returned HTTP 400 (with `"status": 403` inside
  the body). Replaced by REQ-010.AC6.
- **REQ-010.AC5** [target] `POST /api/auth/login` with valid credentials → 200 `{accessToken, refreshToken,
  permissions}`. `permissions` lists the user's effective authorities, for example `["PAYMENT_READ", "PAYMENT_WRITE"]`.
- **REQ-010.AC6** [target] An unknown email or a wrong password → 401 with the same message for both, so the API
  doesn't reveal which emails exist.
- **REQ-010.AC7** [target] A protected endpoint without a token, or with an expired or invalid token → 401.
- **REQ-010.AC8** [target] `POST /api/auth/refresh` with an access token instead of a refresh token, or with an
  invalid token → 401.

### REQ-011 Per-resource read and write permissions
Status: verified · Source: user, 2026-10-04

- **REQ-011.AC1** [target] `GET` on a resource path requires `<RESOURCE>_READ`. `POST`, `PUT`, `PATCH` and `DELETE`
  require `<RESOURCE>_WRITE`.
- **REQ-011.AC2** [target] `WRITE` implies `READ`.
- **REQ-011.AC3** [target] A user without the required permission → 403 with the standard error body, and nothing
  changes.
- **REQ-011.AC4** [target] For every resource, a READ-only user can `GET` but gets 403 on a write. A user with no
  access to that resource gets 403 on both.
- **REQ-011.AC5** [target] Permission changes take effect on the user's next request, without logging in again.

### REQ-012 Named roles
Status: verified · Source: user, 2026-10-04

Roles are named collections of resource permissions. Users get one or more roles. Effective permissions are the union.

- **REQ-012.AC1** [target] `POST /api/roles` `{name, description, permissions: {RESOURCE: READ|WRITE|NONE}}` → 201.
  A duplicate name → 409. An unknown resource or access value → 400.
- **REQ-012.AC2** [target] `GET /api/roles` (optionally `?name=`, exact match) and `GET /api/roles/{id}` return
  roles as `{id, name, description, builtIn, permissions: {RESOURCE: access}}`. Every resource is listed, with
  `NONE` where no access is granted. An unknown id → 404.
- **REQ-012.AC3** [target] `PUT /api/roles/{id}` replaces the name, description and permissions. Users holding the role
  are affected immediately (REQ-011.AC5).
- **REQ-012.AC4** [target] `DELETE /api/roles/{id}` → 204 and removes the role from all users. Deleting the built-in
  `ADMIN` role → 409.
- **REQ-012.AC5** [target] A user holding two roles gets the union, for example STUDENT READ from one role and STUDENT
  WRITE from the other gives WRITE.
- **REQ-012.AC6** [target] A built-in `ADMIN` role exists with WRITE on every resource.

### REQ-013 User administration
Status: verified · Source: user, 2026-10-04

- **REQ-013.AC1** [target] `GET /api/users` lists users `{id, email, roles}`. Password hashes are never returned.
- **REQ-013.AC2** [target] `PUT /api/users/{id}/roles` `{roleIds: [..]}` replaces the user's roles. An unknown role
  ID → 400. An unknown user → 404.
- **REQ-013.AC3** [target] `GET /api/users/me` returns the caller's `{id, email, roles, permissions}`. Any
  authenticated user may call it, whatever their permissions.
- **REQ-013.AC4** [target] An admin cannot remove `ADMIN` from themselves if they are the last admin → 409.

### REQ-014 Self sign-up gives no access
Status: verified · Source: existing code, 2026-10-04

- **REQ-014.AC1** [retired] *(was baseline)* `POST /api/users/signup` returned `{id, email, scope: "NONE"}`.
  Replaced by REQ-014.AC3.
- **REQ-014.AC2** [retired] *(was baseline)* A signed-up user with scope `NONE` could not log in. Replaced by
  REQ-014.AC3: they can log in but have no permissions.
- **REQ-014.AC3** [target] `POST /api/users/signup` → 201 `{id, email, roles: []}`. The user can log in but gets 403 on
  every resource until an admin assigns a role.
- **REQ-014.AC4** [target] Signing up with an email that already exists → 409. A missing email or password → 400.

### REQ-015 Bootstrap administrator
Status: verified (proposed) · Source: design, 2026-10-04

A fresh database (tests, new environments) needs a first admin without editing SQL by hand.

- **REQ-015.AC1** [target] If properties `sonata.bootstrap-admin.email` and `.password` are set and no account with
  that email exists, it is created at startup with the `ADMIN` role. An existing account with that email is left
  unchanged. Without the properties nothing happens.
