# API contract

This folder **is** the specification of the Sonata backend's HTTP API. Each test case is a YAML file entry that says
what request to send and what must come back. The runner (`runner/`, Python) only executes these files. It holds no
knowledge of any specific case, so reading the YAML tells you everything a case checks.

The contract doesn't depend on how the backend is built. Any implementation, in any language, that passes these
specs is a valid Sonata backend.

```
contract/
  contract.yaml      how to start the server and reach its database
  shapes.yaml        named, reusable expectations (e.g. the standard error body)
  fixtures/          reusable setup flows (e.g. "log in as admin")
  specs/<area>.yaml  the test cases, grouped by area
  runner/            the executor (Python 3 + PyYAML)
  run.py             entry point
```

Requirements live in [`docs/requirements/`](../docs/requirements/README.md). Every case lists the acceptance
criteria it proves (`req:`), and the runner reports which criteria are covered and passing.

## Running

```bash
python3 contract/run.py                    # baseline suite (today's behaviour) - must be green
python3 contract/run.py --suite target     # new contract - red until each feature is built
python3 contract/run.py --suite all
python3 contract/run.py --only STU-003     # one case (or a prefix: --only STU-)
python3 contract/run.py --file students    # one spec file
python3 contract/run.py --no-build         # skip rebuilding the backend
python3 contract/run.py --base-url http://localhost:8080 --no-server   # use a server you started yourself
```

The run writes `contract/.out/report.md` (results and requirement coverage) and `contract/.out/server.log`.

What a run does:
1. Builds and starts the backend against a fresh, empty database (`contract.yaml` → `server`, `database`).
2. Before **each case**, empties every table except the ones listed in `database.preserveTables`, so cases never
   see each other's data.
3. Runs the file's `background` steps, then the case's `steps`, in order. The first failing expectation fails the
   case and its message shows exactly what differed.

## Spec file layout

```yaml
area: Students                 # heading in reports
suite: baseline                # default suite for cases in this file: baseline | target
background:                    # steps run before every case in this file
  - use: baseline-admin        # runs fixtures/baseline-admin.yaml (saves {{admin}} = a token)

cases:
  - id: STU-001                # stable, unique id
    title: First student gets admission number 1
    req: [REQ-020.AC1]         # acceptance criteria this case proves
    suite: baseline            # optional override of the file default
    steps:
      - POST: /api/students
        auth: admin            # send "Authorization: Bearer {{admin}}"
        body: { studentName: Asha }
        expect:
          status: 200
          body: { admissionNo: 1, studentName: Asha }
```

**Suites:**
- `baseline` describes how the API behaves today. These cases must pass on every commit.
- `target` describes the new contract. These cases are red until the feature is built.

A requirement change that alters behaviour deletes or edits the baseline case in the same commit.

## Steps

### HTTP request

```yaml
- PUT: /api/students/{{sid}}       # method as the key: GET, POST, PUT, PATCH, DELETE
  auth: admin                      # variable holding a bearer token; omit for an anonymous call
  query: { page: 1, size: 5 }      # optional query string
  headers: { X-Trace: abc }        # optional extra headers
  body: { contactNo: "123" }       # JSON body (or rawBody: "<text>" to send text as-is)
  expect: { status: 200 }          # see "Expectations"
  save:                            # store values for later steps
    sid: body.admissionNo          # dotted path into the response; list items by index: body.content.0.id
    code: status
```

### Database statement

Used for setup that the API can't do (today's scope-ANY admin), and to prove database-level guarantees.

```yaml
- sql: SELECT count(*) AS n FROM payments WHERE student_id = {{sid}}
  expect:
    rows: [ { n: 3 } ]             # rows matched like a JSON array (see Expectations)
- sql: INSERT INTO payments (...) VALUES (...)
  expect:
    error: { $contains: check constraint }   # statement must FAIL with a matching message
```

### Reuse, repetition, waiting

```yaml
- use: baseline-admin              # run fixtures/baseline-admin.yaml in place
  with: { email: x@y.z }           # optional values for the fixture's {{params}}

- POST: /api/students              # repeat this step N times; {{i}} = 1..N
  repeat: 12
  auth: admin
  body: { studentName: "Student {{i}}" }
  expect: { status: 200 }

- GET: /api/dues/student/{{sid}}   # retry until the expectation holds (for background work)
  eventually: 10s
  auth: admin
  expect: { status: 200, body: { $len: 4 } }
```

### Examples (one case, several inputs)

```yaml
- id: STU-010
  title: Blank student name is rejected
  examples:                        # the case runs once per row; row values become variables
    - { name: "" }
    - { name: "   " }
  steps:
    - POST: /api/students
      auth: admin
      body: { studentName: "{{name}}" }
      expect: { status: 400 }
```

## Values and variables

`{{name}}` inserts a saved variable, a fixture parameter or an example value. If the whole value is a single
`{{...}}`, the original type is kept (a number stays a number).

| Expression | Meaning |
|---|---|
| `{{sid}}`, `{{student.admissionNo}}` | saved variable (dotted path into a saved object) |
| `{{today}}` | today's date, `2026-10-04` |
| `{{today -1d}}`, `{{today +2m}}`, `{{today -1y}}` | date offset by days, months or years |
| `{{now}}`, `{{now -30d}}` | local date-time, `2026-10-04T10:15:00` |
| `{{year}}`, `{{year -1}}` | current year (number) |
| `{{uniq}}` | a value unique within the run, e.g. `u1739` (for emails and role names) |

## Expectations

```yaml
expect:
  status: 201                 # exact status, or a matcher: { $oneOf: [200, 201] }
  body: ...                   # matched against the JSON response body (rules below)
  text: OK                    # for non-JSON bodies: whole body text
```

**Matching rules (expected vs actual):**

| Expected | Passes when |
|---|---|
| `42`, `"Asha"`, `true` | actual is equal (`42` equals `42.0`) |
| `null` | actual is JSON null |
| `{ a: 1, b: "x" }` | actual is an object with **at least** these keys matching. Other keys are ignored |
| `[ x, y ]` | actual is a list of **exactly** 2 items, matching in order |

**Operators** (an object whose keys start with `$`). Every operator listed must hold:

| Operator | Passes when |
|---|---|
| `{ $absent: true }` | as a field value: the field is not present at all |
| `{ $present: true }` | the field is present (any value, even null) |
| `{ $type: string }` | type is `string`, `number`, `integer`, `boolean`, `object`, `array` or `null` |
| `{ $ne: x }` | not equal to x |
| `{ $oneOf: [a, b] }` | equal to one of the values |
| `{ $gt: 0 }` `$gte` `$lt` `$lte` | numeric (or ISO date/time string) comparison |
| `{ $regex: "^\\d+$" }` | string matches the regular expression |
| `{ $contains: "abc" }` | string contains the text, case-insensitive |
| `{ $notBlank: true }` | string with at least one non-space character |
| `{ $len: 3 }` | list, string or object has this length (a matcher is allowed: `{ $len: { $gte: 1 } }`) |
| `{ $has: [x, y] }` | list includes an item matching each of x, y (any order, other items allowed) |
| `{ $unordered: [x, y] }` | list has exactly these items, in any order |
| `{ $every: x }` | every item of the list matches x (an empty list passes) |
| `{ $none: x }` | no item of the list matches x |
| `{ $only: { a: 1 } }` | object has exactly these keys, no others |
| `{ $eq: { $len: 1 } }` | literal comparison, for data that itself looks like an operator |
| `{ $not: x }` | does not match x |
| `{ $shape: error }` | matches the named shape defined in [`shapes.yaml`](shapes.yaml). Keys next to it add more checks: `{ $shape: error, status: 409 }` |
