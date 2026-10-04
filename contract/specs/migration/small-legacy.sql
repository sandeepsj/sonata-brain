-- A small database in the LEGACY (production, 2026-10-04) shape, with known content.
-- The migration specs start the new backend on top of it and check what the migration produced.
--
-- Courses:  VIOLIN, KEYBOARD
-- Fees:     VIOLIN 2024 - 3 terms (Jan-Apr, May-Aug, Sep-Dec) of 1400        -> rows 1, 2, 3
--           VIOLIN 2025 - 4 terms (quarters) of 1000, 1000, 1100, 1100       -> rows 4, 5, 6, 7
--           KEYBOARD 2025 - 4 terms of 1050                                   -> rows 8, 9, 10, 11
-- Students: 1 Asha  - VIOLIN from 10 May 2024, still enrolled                 (session 1)
--           2 Ravi  - KEYBOARD from 1 Feb 2025 to 15 Aug 2025                 (session 2)
--           3 Meera - no session, one additional payment
-- Payments: 1 Asha  REGULAR  VIOLIN 2025 T1  1000                  (Jan 2025)
--           2 Asha  REGULAR  VIOLIN 2025 T2   600 + 100 discount    (Apr 2025)
--           3 Asha  REGULAR  VIOLIN 2024 T2  1400                  (Jun 2024)
--           4 Asha  ADDITIONAL Admission fee 500, no course
--           5 Ravi  REGULAR  KEYBOARD 2025 T1 1050
--           6 Ravi  REGULAR  KEYBOARD 2025 T2  300, DEACTIVATED
--           7 Meera ADDITIONAL Books 250, course VIOLIN
-- Dues (as the legacy code left them):
--           Asha  2024 T2 PAID, 2024 T3 DUE 1400, 2025 T1 PAID, T2 PARTIALLY_PAID 300, T3 DUE 1100, T4 DUE 1100
--           Ravi  2025 T1 PAID, T2 DUE 1050, T3 STUDENT_LEFT_DUE_CANCELLED, T4 CANCELLED
-- Accounts: admin@sonata.local (scope ANY, password legacy-admin-1), staff@sonata.local (scope NONE, legacy-staff-1)

\ir legacy-schema.sql

INSERT INTO account (user_id, email, password, scope) VALUES
  (1, 'admin@sonata.local', '$2a$10$ZoB6h9MWWjoSEFMWRplfY.i.feCf7wFpWuAUyKQRv5dSmy9G4/q3e', 'ANY'),
  (2, 'staff@sonata.local', '$2a$10$mkhr6LUSSnqUx/Tudz7RyOIUMmpA7Cgw.FAyM8eZ2NjLZr65lnyJm', 'NONE');

INSERT INTO course (name) VALUES ('VIOLIN'), ('KEYBOARD');

INSERT INTO fee_structure (id, course_id, year, term, amount, is_active) VALUES
  (1, 'VIOLIN', 2024, 1, 1400, true), (2, 'VIOLIN', 2024, 2, 1400, true), (3, 'VIOLIN', 2024, 3, 1400, true),
  (4, 'VIOLIN', 2025, 1, 1000, true), (5, 'VIOLIN', 2025, 2, 1000, true),
  (6, 'VIOLIN', 2025, 3, 1100, true), (7, 'VIOLIN', 2025, 4, 1100, true),
  (8, 'KEYBOARD', 2025, 1, 1050, true), (9, 'KEYBOARD', 2025, 2, 1050, true),
  (10, 'KEYBOARD', 2025, 3, 1050, true), (11, 'KEYBOARD', 2025, 4, 1050, true);

INSERT INTO student (admission_no, student_name, status, admission_date, contact_no) VALUES
  (1, 'Asha', 'active', '2024-05-10', '9800000001'),
  (2, 'Ravi', 'active', '2025-02-01', '9800000002'),
  (3, 'Meera', 'active', '2025-03-01', '9800000003');

INSERT INTO student_sessions (id, student_id, course_name, date_of_joining, date_of_disjoining) VALUES
  (1, 1, 'VIOLIN', '2024-05-10 10:00:00', NULL),
  (2, 2, 'KEYBOARD', '2025-02-01 10:00:00', '2025-08-15 10:00:00');

INSERT INTO payments (id, student_id, payment_type, term_id, course_name, amount_paid, discount_amount, label, remarks,
                      date, is_active, created_at, updated_at) VALUES
  (1, 1, 'REGULAR', 4, 'VIOLIN', 1000, 0, 'Course fee', NULL, '2025-01-15 10:00:00', true, '2025-01-15 10:00:00', NULL),
  (2, 1, 'REGULAR', 5, 'VIOLIN', 600, 100, 'Course fee', 'sibling discount', '2025-04-10 10:00:00', true, '2025-04-10 10:00:00', NULL),
  (3, 1, 'REGULAR', 2, 'VIOLIN', 1400, 0, 'Course fee', NULL, '2024-06-01 10:00:00', true, '2024-06-01 10:00:00', NULL),
  (4, 1, 'ADDITIONAL', NULL, NULL, 500, 0, 'Admission fee', NULL, '2024-05-10 10:00:00', true, '2024-05-10 10:00:00', NULL),
  (5, 2, 'REGULAR', 8, 'KEYBOARD', 1050, 0, 'Course fee', NULL, '2025-02-05 10:00:00', true, '2025-02-05 10:00:00', NULL),
  (6, 2, 'REGULAR', 9, 'KEYBOARD', 300, 0, 'Course fee', 'entered twice', '2025-04-02 10:00:00', false, '2025-04-02 10:00:00', '2025-04-03 10:00:00'),
  (7, 3, 'ADDITIONAL', NULL, 'VIOLIN', 250, 0, 'Books', NULL, '2025-03-01 10:00:00', true, '2025-03-01 10:00:00', NULL);

INSERT INTO discounts (id, payment_id, discount_amount, label, remarks, created_at) VALUES
  (1, 2, 100, 'Sibling', NULL, '2025-04-10 10:00:00');

INSERT INTO fee_dues_tracker (id, student_session_id, fee_structure_id, due_amount, status, created_at) VALUES
  (1, 1, 2, 0, 'PAID', '2024-05-10 10:00:00'),
  (2, 1, 3, 1400, 'DUE', '2024-09-01 10:00:00'),
  (3, 1, 4, 0, 'PAID', '2025-01-01 10:00:00'),
  (4, 1, 5, 300, 'PARTIALLY_PAID', '2025-01-01 10:00:00'),
  (5, 1, 6, 1100, 'DUE', '2025-01-01 10:00:00'),
  (6, 1, 7, 1100, 'DUE', '2025-01-01 10:00:00'),
  (7, 2, 8, 0, 'PAID', '2025-02-01 10:00:00'),
  (8, 2, 9, 1050, 'DUE', '2025-02-01 10:00:00'),
  (9, 2, 10, 1050, 'STUDENT_LEFT_DUE_CANCELLED', '2025-02-01 10:00:00'),
  (10, 2, 11, 1050, 'CANCELLED', '2025-02-01 10:00:00');

-- Identity sequences continue after the explicit ids, as they do in production.
SELECT setval('account_user_id_seq', 2), setval('fee_structure_id_seq', 11), setval('student_admission_no_seq', 3),
       setval('student_sessions_id_seq', 2), setval('payments_id_seq', 7), setval('discounts_id_seq', 1),
       setval('fee_dues_tracker_id_seq', 10);
