-- REQ-083: compare the legacy dues with the recalculated ones, per student. Read-only.
-- Run after V5 and the first recalculation:  psql "$DB" -f scripts/db/compare-dues.sql
--
--   legacy_outstanding  sum of legacy due_amount still DUE or PARTIALLY_PAID (the legacy remaining balance)
--   new_balance         sum of fee_due.balance
--   unapplied           active REGULAR money (amount + discounts) not applied to any due: credit, plus discounts
--                       left over after every due is paid
--   difference          new_balance - legacy_outstanding

\echo '== Totals'
WITH legacy AS (
    SELECT ss.student_id, sum(l.due_amount) AS outstanding
    FROM legacy_fee_dues l JOIN student_sessions ss ON ss.id = l.student_session_id
    WHERE l.status IN ('DUE', 'PARTIALLY_PAID')
    GROUP BY ss.student_id),
fresh AS (
    SELECT student_id, sum(amount) AS billed, sum(paid_amount + discount_amount) AS applied, sum(balance) AS balance
    FROM fee_due GROUP BY student_id),
money AS (
    SELECT student_id, sum(amount_paid + coalesce(discount_amount, 0)) AS paid
    FROM payments WHERE payment_type = 'REGULAR' AND is_active GROUP BY student_id),
per_student AS (
    SELECT s.admission_no, s.student_name,
           coalesce(l.outstanding, 0) AS legacy_outstanding,
           coalesce(f.balance, 0) AS new_balance,
           coalesce(m.paid, 0) - coalesce(f.applied, 0) AS unapplied
    FROM student s
    LEFT JOIN legacy l ON l.student_id = s.admission_no
    LEFT JOIN fresh f ON f.student_id = s.admission_no
    LEFT JOIN money m ON m.student_id = s.admission_no)
SELECT count(*) AS students,
       count(*) FILTER (WHERE new_balance <> legacy_outstanding) AS students_that_differ,
       sum(legacy_outstanding) AS legacy_outstanding,
       sum(new_balance) AS new_balance,
       sum(new_balance) - sum(legacy_outstanding) AS difference,
       sum(unapplied) AS unapplied
FROM per_student;

\echo '== Students whose outstanding amount changed (largest first)'
WITH legacy AS (
    SELECT ss.student_id, sum(l.due_amount) AS outstanding
    FROM legacy_fee_dues l JOIN student_sessions ss ON ss.id = l.student_session_id
    WHERE l.status IN ('DUE', 'PARTIALLY_PAID')
    GROUP BY ss.student_id),
fresh AS (
    SELECT student_id, sum(paid_amount + discount_amount) AS applied, sum(balance) AS balance,
           min(period_start) FILTER (WHERE balance > 0) AS oldest_open
    FROM fee_due GROUP BY student_id),
money AS (
    SELECT student_id, sum(amount_paid + coalesce(discount_amount, 0)) AS paid
    FROM payments WHERE payment_type = 'REGULAR' AND is_active GROUP BY student_id)
SELECT s.admission_no, s.student_name,
       coalesce(l.outstanding, 0) AS legacy_outstanding,
       coalesce(f.balance, 0) AS new_balance,
       coalesce(f.balance, 0) - coalesce(l.outstanding, 0) AS difference,
       coalesce(m.paid, 0) - coalesce(f.applied, 0) AS unapplied,
       f.oldest_open
FROM student s
LEFT JOIN legacy l ON l.student_id = s.admission_no
LEFT JOIN fresh f ON f.student_id = s.admission_no
LEFT JOIN money m ON m.student_id = s.admission_no
WHERE coalesce(f.balance, 0) <> coalesce(l.outstanding, 0)
ORDER BY abs(coalesce(f.balance, 0) - coalesce(l.outstanding, 0)) DESC, s.admission_no;
