# Payments

Payment types: `REGULAR` (course fee for a term) and `ADDITIONAL` (anything else: admission fee, books, exams…).
Payments are the source of truth for money received. They are never deleted, only deactivated.

### REQ-060 Regular (course fee) payment
Status: verified · Source: existing code; contract changed by user decision, 2026-10-04

- **REQ-060.AC1** [retired] *(was baseline; replaced by REQ-060.AC5 to AC8)* `POST /api/payments` `{studentId, amountPaid, termId, courseName, label?, remarks?,
  date?, discounts?}` (a `termId` makes it REGULAR) → 201 with `paymentType: "REGULAR"`.
- **REQ-060.AC2** [retired] *(was baseline; replaced by REQ-060.AC5 to AC8)* An unknown student, course or `termId` → 400. A `courseName` that doesn't match the
  fee structure's course → 400.
- **REQ-060.AC3** [retired] *(was baseline; replaced by REQ-060.AC5 to AC8)* If the student has no session overlapping the term → 400.
- **REQ-060.AC4** [retired] *(was baseline; replaced by REQ-060.AC5 to AC8)* If `amountPaid` + discounts + previous active payments for that term exceed the term
  amount → 400.
- **REQ-060.AC5** [target] `POST /api/payments` `{studentId, paymentType: "REGULAR", courseName, periodDate,
  amountPaid, discounts?, date?, label?, remarks?}` → 201. `periodDate` is any date inside the term being paid for.
- **REQ-060.AC6** [target] A REGULAR payment without `courseName` or `periodDate` → 400.
- **REQ-060.AC7** [target] If `periodDate` falls in no billing period of the course timeline → 400. If the student
  had no session overlapping that period → 400.
- **REQ-060.AC8** [target] If `amountPaid` + discounts exceeds that period's current balance → 400, and the message
  states the balance.
- **REQ-060.AC9** [target] A REGULAR payment for a CLOSED course → 409.
- **REQ-060.AC10** [target] Creating a REGULAR payment rebuilds that student's dues (REQ-074).
- **REQ-060.AC11** [retired] *(was baseline-defect; replaced by REQ-060.AC12)* A REGULAR payment without `label` fails with 500, because `payments.label` is
  NOT NULL in the database (prod too) while the code treats the label as optional. All 2,539 prod payments have a
  label. Fixed by REQ-060.AC12.
- **REQ-060.AC12** [target] *(proposed)* `label` is required for every payment type. A missing or blank label → 400
  with a `details` entry for `label`.

### REQ-061 Additional payment
Status: verified · Source: existing code; user clarification 2026-10-04

- **REQ-061.AC1** [baseline] An ADDITIONAL payment with a `label` → 201. `courseName` is optional but must exist if
  given. (Before the redesign, "no `termId`" made a payment ADDITIONAL; now `paymentType` says so.)
- **REQ-061.AC2** [baseline] An ADDITIONAL payment without a `label` (missing or blank) → 400.
- **REQ-061.AC3** [target] `POST /api/payments` `{paymentType: "ADDITIONAL", studentId, label, amountPaid,
  courseName?}` → 201 with `periodDate: null`. Sending a `periodDate` → 400.
- **REQ-061.AC4** [target] ADDITIONAL payments never affect dues.
- **REQ-061.AC5** [target] The database refuses a REGULAR payment without a course or period (CHECK constraint),
  even if the API is bypassed.

### REQ-062 Zero-amount payments
Status: verified · Source: existing code

- **REQ-062.AC1** [baseline] `amountPaid: 0` is accepted only if at least one discount is given, otherwise → 400.
- **REQ-062.AC2** [baseline] A negative `amountPaid` → 400. A missing `amountPaid` or `studentId` → 400.

### REQ-063 Discounts
Status: verified · Source: existing code

- **REQ-063.AC1** [baseline] `discounts: [{discountAmount, label, remarks?}]` are stored against the payment. The
  payment's `discountAmount` equals their sum.
- **REQ-063.AC2** [retired] *(was baseline-defect; replaced by REQ-063.AC4)* Discount fields aren't validated today. A discount amount of 0 or less is
  accepted as given, and a missing label fails with a 500. Fixed by REQ-063.AC4.
- **REQ-063.AC3** [baseline] `GET /api/discounts/payment/{paymentId}` lists a payment's discounts. `GET
  /api/discounts/{id}` → 200 or 404.
- **REQ-063.AC4** [target] A discount with an amount of 0 or less, or a blank label → 400 with a `details` entry for
  that field. Nothing is stored.

### REQ-064 Deactivate a payment
Status: verified · Source: existing code

- **REQ-064.AC1** [baseline] `PATCH /api/payments/{id}/deactivate` → 200 with `isActive: false`.
- **REQ-064.AC2** [baseline] Deactivating an already inactive payment → 409 (400 before REQ-064.AC4). An unknown
  payment → 404.
- **REQ-064.AC3** [baseline] A deactivated payment can't be reactivated (there's no endpoint for it).
- **REQ-064.AC4** [target] Deactivating an already inactive payment → 409.
- **REQ-064.AC5** [target] Deactivating a REGULAR payment rebuilds dues, so the period balance goes back up by
  amount + discounts.
- **REQ-064.AC6** [retired] *(was baseline-defect; replaced by REQ-064.AC7)* Deactivating a REGULAR payment races with the background dues job. Both lazily
  load the same payment's `discounts` at once, so the request intermittently fails with 500, or the due is not
  restored ("Illegal pop() with non-matching JdbcValuesSourceProcessingState" in the log). Reproduced in 5 of 6 runs.
  Fixed by REQ-064.AC7.
- **REQ-064.AC7** [target] Deactivation never fails or loses a dues update because of background work. Repeating
  the deactivate-then-check-dues flow 20 times succeeds every time.

### REQ-065 Convert an additional payment into a course fee
Status: verified · Source: user ("admin staff sometimes mark payments as additional instead of course fee"), 2026-10-04

- **REQ-065.AC1** [target] `POST /api/payments/{id}/convert-to-course-fee` `{courseName?, periodDate}` → 200. The
  payment becomes `paymentType: "REGULAR"` with `courseName` and `periodDate` set, `originalType: "ADDITIONAL"`,
  `convertedAt` = now, and `convertedBy` = the caller's email.
- **REQ-065.AC2** [target] `courseName` may be left out if the payment already has one. If neither is present → 400.
- **REQ-065.AC3** [target] The same checks as REQ-060.AC7 to AC9 apply: the period exists, the session overlaps it,
  the amount fits the balance, and the course is active.
- **REQ-065.AC4** [target] Converting a payment that is already REGULAR → 409. An inactive payment → 409. An unknown
  payment → 404.
- **REQ-065.AC5** [target] After conversion, the student's dues show the payment applied to that period.
- **REQ-065.AC6** [target] The payment ID, amount, date, discounts and remarks are unchanged by conversion.

### REQ-066 Query payments
Status: verified · Source: existing code

- **REQ-066.AC1** [retired] *(was baseline; replaced by REQ-066.AC4 and GET /api/payments/{id})* `GET /api/payments/{id}` → 200 or 404. `GET /api/payments/student/{id}` lists all of a
  student's payments.
- **REQ-066.AC2** [retired] *(was baseline; replaced by REQ-066.AC4 and GET /api/payments/{id})* `GET /api/payments/student/{id}/type/{REGULAR|ADDITIONAL}` and
  `/student/{id}/active` filter by type and by active status.
- **REQ-066.AC3** [retired] *(was baseline; replaced by REQ-066.AC4 and GET /api/payments/{id})* `GET /api/payments/date-range?startDate=&endDate=` returns active payments in the range,
  newest first. `/date-range/course/{name}` and `/date-range/type/{type}` narrow it further.
- **REQ-066.AC4** [target] `GET /api/payments?studentId=&courseName=&type=&active=&from=&to=&periodDate=` replaces the
  path variants. `from` and `to` filter the payment date, inclusive. `periodDate` returns REGULAR payments for the
  billing period that contains that date. Results are ordered newest first (by payment date), and each payment
  includes its discounts. An invalid `type` → 400.
