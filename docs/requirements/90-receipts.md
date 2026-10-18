# Receipt templates

A receipt template is a complete **HTML document**: markup, CSS (including `@page` for the paper size) and, if
wanted, `<script>`. Users edit it in the frontend's editor mode. The HTML refers to **variables** in Handlebars
syntax; at print time the frontend fills them in from the receipt data and renders the result. The rendered page is
the receipt. Users keep a list of templates and pick one when printing; one of them is the institute-wide default.

The backend only stores templates. It does not parse, sanitise or render the HTML.

Not in scope:
- server-side rendering or PDF generation;
- storing printed receipts or receipt numbers;
- per-user default templates (the default is institute-wide);
- template versions or history.

## Variables (owned by the frontend, listed here for reference)

The frontend builds this object from the existing endpoints (student, payments, discounts) and shows the list in the
editor. Handlebars fills `{{…}}`; scripts read the same object as `window.receipt`. Amounts are formatted for India
(`1,23,450`, no `₹`) and dates look like `18 Oct 2026`.

| Variable | Meaning |
|---|---|
| `receipt.id` | receipt number shown on the receipt |
| `receipt.printedDate` | the date the receipt is printed |
| `student.name`, `student.admissionNo` | the student |
| `payments` | the payments on the receipt, oldest first; use `{{#each payments}}…{{/each}}` |
| `date`, `courseName`, `label` | inside `each payments`: payment date, course (`Fee Payment` when none), label |
| `fee`, `discount`, `amountPaid` | inside `each payments`: fee before discount, discount (empty when none), cash paid |
| `hasDiscounts`, `discounts` | inside `each payments`: whether discounts apply; each discount has `label` and `amount` |
| `totals.fee`, `totals.discount`, `totals.paid` | sums over the payments |

The frontend may add variables later; templates that don't use them keep working.

### REQ-090 Receipt templates
Status: accepted · Source: user, 2026-10-18

- **REQ-090.AC1** [target] `GET /api/receipt-templates` → 200 with every template, the built-in one first, then by
  name ignoring case. Each item is `{id, name, builtIn, isDefault, createdAt, createdBy, updatedAt, updatedBy}`
  (no `html`, so the list stays light); `*By` is the user's email.
- **REQ-090.AC2** [target] `GET /api/receipt-templates/{id}` → 200 with the template including `html`, or 404.
- **REQ-090.AC3** [target] `POST /api/receipt-templates {name, html}` → 201 with the template including `html`,
  `builtIn: false`, `isDefault: false`. The name is stored trimmed.
- **REQ-090.AC4** [target] `PUT /api/receipt-templates/{id} {name, html}` replaces the name and the HTML → 200, with
  `updatedAt` and `updatedBy` refreshed. Unknown id → 404; the built-in template → 409.
- **REQ-090.AC5** [target] `DELETE /api/receipt-templates/{id}` → 204. Unknown id → 404; the built-in template → 409.
  Deleting the default template makes the built-in template the default.
- **REQ-090.AC6** [target] A missing or blank name, or a name longer than 80 characters → 400 with a `details` entry
  for `name`. A name equal to another template's, ignoring case and surrounding spaces → 409.
- **REQ-090.AC7** [target] `html` missing, not a string, blank, or larger than 1 MB (UTF-8) → 400 with a `details`
  entry for `html`. Name and `html` problems are reported together. Nothing else about the HTML is checked: any
  markup, CSS and scripts are accepted.
- **REQ-090.AC8** [target] `html` comes back exactly as saved, character for character, including scripts, non-ASCII
  text and whitespace.
- **REQ-090.AC9** [process] The frontend renders and prints a template only inside a sandboxed iframe
  (`sandbox="allow-scripts allow-modals"`, never `allow-same-origin`), so a template's script can't read the user's
  token or call the API.

### REQ-091 Default and built-in template
Status: accepted · Source: user, 2026-10-18

- **REQ-091.AC1** [target] A migration seeds one template named `Standard` with `builtIn: true` and
  `isDefault: true`. Its HTML reproduces the receipt as printed before templates existed: A5 landscape, Arial, the
  institute header, receipt number, admission number and name, the payments table with discount notes, the total,
  the printed date and the `Secretary/Clerk` signature line.
- **REQ-091.AC2** [target] `PATCH /api/receipt-templates/{id}/default` → 200 with the template, now `isDefault: true`.
  The previous default becomes `isDefault: false` in the same transaction. Making the default the default again is
  allowed (200, nothing changes). Unknown id → 404. The built-in template may be made default again.
- **REQ-091.AC3** [target] There is always exactly one default template (a partial unique index allows at most one,
  and the rules above never leave none).
- **REQ-091.AC4** [target] `GET /api/receipt-templates/default` → 200 with the default template including `html`, so
  printing needs one call.

### REQ-092 Access to receipt templates
Status: accepted · Source: user, 2026-10-18

- **REQ-092.AC1** [target] `/api/receipt-templates/**` is a catalogue route guarded like the others
  (REQ-011.AC6–AC10): `GET` needs `read`; `POST`, `PUT`, `PATCH` and `DELETE` need `write`.
- **REQ-092.AC2** [target] The built-in `ADMIN` role has read and write on it.
- **REQ-092.AC3** [manual] The migration grants every existing role the same read and write on this route as it has
  on `/api/payments/**`: anyone who can print receipts can use and save templates. Checked on a copy of production:
  every role's `/api/receipt-templates/**` flags equal its `/api/payments/**` flags.
- **REQ-092.AC4** [target] It appears in every role's `routes` in `GET /api/roles` (REQ-012.AC10), like every other
  catalogue path.
