# Payments

Payment types: `REGULAR` (course fee) and `ADDITIONAL` (anything else: admission fee, books, exams…).
Payments are the source of truth for money received. They are never deleted, only deactivated. Since 2026-10-15 a
payment doesn't name a term: it is a date, an amount, a course and discounts, and dues apply it oldest term first
(REQ-071.AC6).

### REQ-060 Regular (course fee) payment
Status: accepted · Source: existing code; contract changed by user decision, 2026-10-04

- **REQ-060.AC1** [retired] *(was baseline)* `POST /api/payments` with a `termId` made a REGULAR payment. Replaced by REQ-060.AC13.
- **REQ-060.AC2** [retired] *(was baseline)* Unknown `termId` or a course not matching the term → 400. Terms are gone; see REQ-060.AC14.
- **REQ-060.AC3** [retired] *(was baseline)* No session overlapping the term → 400. Replaced by REQ-060.AC14.
- **REQ-060.AC4** [retired] *(was baseline)* Payment can't exceed the term amount. Dropped: overpayment becomes credit (REQ-071.AC7).
- **REQ-060.AC5** [retired] `periodDate` design. Replaced by REQ-060.AC13.
- **REQ-060.AC6** [retired] `periodDate` required. Replaced by REQ-060.AC14.
- **REQ-060.AC7** [retired] `periodDate` checks. Replaced by REQ-060.AC14.
- **REQ-060.AC8** [retired] Amount limited to the period balance. Dropped: overpayment becomes credit.
- **REQ-060.AC9** [target] A REGULAR payment for a CLOSED course → 409.
- **REQ-060.AC10** [target] Creating a REGULAR payment rebuilds that student's dues (REQ-074).
- **REQ-060.AC11** [retired] *(was baseline-defect)* A REGULAR payment without `label` failed with 500. Fixed by REQ-060.AC12.
- **REQ-060.AC12** [target] `label` is required for every payment type. A missing or blank label → 400
  with a `details` entry for `label`.
- **REQ-060.AC13** [target] `POST /api/payments` `{studentId, paymentType: "REGULAR", courseName, amountPaid, label,
  date?, remarks?, discounts?}` → 201 with the payment. There is no `termId` and no `periodDate`. `date` defaults to
  now.
- **REQ-060.AC14** [target] A REGULAR payment needs `courseName` → otherwise 400. An unknown student or course → 400.
  The student must have (or have had) a session in that course → otherwise 400. There is no upper limit: money beyond
  what is due becomes credit (REQ-071.AC7).

### REQ-061 Additional payment
Status: accepted · Source: existing code; user clarification 2026-10-04

- **REQ-061.AC1** [retired] *(was baseline)* No `termId` made a payment ADDITIONAL. Replaced by REQ-061.AC6.
- **REQ-061.AC2** [retired] *(was baseline)* "Additional with termId" impossible. Replaced by REQ-061.AC6.
- **REQ-061.AC3** [retired] ADDITIONAL with `periodDate: null`. Replaced by REQ-061.AC6.
- **REQ-061.AC4** [target] ADDITIONAL payments never affect dues.
- **REQ-061.AC5** [retired] DB check on course and period. Replaced by REQ-061.AC7.

- **REQ-061.AC6** [target] `paymentType` is required and is `REGULAR` or `ADDITIONAL` → otherwise 400 with a
  `details` entry for `paymentType`. An ADDITIONAL payment's `courseName` is optional but must exist if given.
- **REQ-061.AC7** [manual] The database refuses a REGULAR payment without a course (CHECK constraint), even if the
  API is bypassed.

### REQ-062 Zero-amount payments
Status: accepted · Source: existing code

- **REQ-062.AC1** [baseline] `amountPaid: 0` is accepted only if at least one discount is given, otherwise → 400.
- **REQ-062.AC2** [baseline] A negative `amountPaid` → 400. A missing `amountPaid` or `studentId` → 400.

### REQ-063 Discounts
Status: accepted · Source: existing code

- **REQ-063.AC1** [baseline] `discounts: [{discountAmount, label, remarks?}]` are stored against the payment. The
  payment's `discountAmount` equals their sum.
- **REQ-063.AC2** [baseline-defect] Discount fields aren't validated today. A discount amount of 0 or less is
  accepted as given, and a missing label fails with a 500. Fixed by REQ-063.AC4.
- **REQ-063.AC3** [baseline] `GET /api/discounts/payment/{paymentId}` lists a payment's discounts. `GET
  /api/discounts/{id}` → 200 or 404.
- **REQ-063.AC4** [target] A discount with an amount of 0 or less, or a blank label → 400 with a `details` entry for
  that field. Nothing is stored.

### REQ-064 Deactivate a payment
Status: accepted · Source: existing code

- **REQ-064.AC1** [baseline] `PATCH /api/payments/{id}/deactivate` → 200 with `isActive: false`.
- **REQ-064.AC2** [baseline] Deactivating an already inactive payment → 400. An unknown payment → 404.
- **REQ-064.AC3** [baseline] A deactivated payment can't be reactivated (there's no endpoint for it).
- **REQ-064.AC4** [target] Deactivating an already inactive payment → 409.
- **REQ-064.AC5** [target] Deactivating a REGULAR payment rebuilds dues, so the period balance goes back up by
  amount + discounts.
- **REQ-064.AC6** [retired] *(was baseline-defect)* Deactivation raced with the background dues job. Fixed by
  REQ-064.AC7: dues are now recalculated synchronously in the same transaction.
- **REQ-064.AC7** [target] Deactivation never fails or loses a dues update because of background work. Repeating
  the deactivate-then-check-dues flow 20 times succeeds every time.

### REQ-065 Convert an additional payment into a course fee
Status: accepted · Source: user ("admin staff sometimes mark payments as additional instead of course fee"), 2026-10-04

- **REQ-065.AC1** [retired] Convert with `periodDate`. Replaced by REQ-065.AC7.
- **REQ-065.AC2** [target] `courseName` may be left out if the payment already has one. If neither is present → 400.
- **REQ-065.AC3** [retired] `periodDate` checks on conversion. Replaced by REQ-065.AC7.
- **REQ-065.AC4** [target] Converting a payment that is already REGULAR → 409. An inactive payment → 409. An unknown
  payment → 404.
- **REQ-065.AC5** [target] After conversion, the student's dues show the payment applied to that period.
- **REQ-065.AC6** [target] The payment ID, amount, date, discounts and remarks are unchanged by conversion.
- **REQ-065.AC7** [target] `POST /api/payments/{id}/convert-to-course-fee` `{courseName?}` → 200; the payment becomes
  REGULAR with the same checks as REQ-060.AC14, and dues are recalculated. *(not built yet)*

### REQ-066 Query payments
Status: accepted · Source: existing code

- **REQ-066.AC1** [baseline] `GET /api/payments/{id}` → 200 or 404. `GET /api/payments/student/{id}` lists all of a
  student's payments.
- **REQ-066.AC2** [baseline] `GET /api/payments/student/{id}/type/{REGULAR|ADDITIONAL}` and
  `/student/{id}/active` filter by type and by active status.
- **REQ-066.AC3** [baseline] `GET /api/payments/date-range?startDate=&endDate=` returns active payments in the range,
  newest first. `/date-range/course/{name}` and `/date-range/type/{type}` narrow it further.
- **REQ-066.AC4** [retired] Query endpoint with `periodDate`. Replaced by REQ-066.AC5.
- **REQ-066.AC5** [target] `GET /api/payments?studentId=&courseName=&type=&active=&from=&to=` replaces the path
  variants, newest first, each with its discounts. *(not built yet)*
