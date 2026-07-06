-- ============================================================================
-- Bank Management System - sample data
--
-- Run once against a fresh database, after sql/create_tables.sql:
--     psql -d bankdb -f sql/insert_data.sql
--
-- Seeds 3 employees, 16 clients and their 16 accounts (5 regular checking,
-- 4 business checking, 4 savings, 3 youth), 2 loans, 2 mortgages, 13 cards
-- and 8 deposit/withdrawal movements. Balances are consistent with the
-- movement log (e.g. account 1000: +500 -200 = 300; account 1006 is 800
-- into its overdraft). Youth birth dates are computed relative to today so
-- the program's under-18 rule keeps holding no matter when this file runs.
-- ============================================================================

BEGIN;

INSERT INTO employees (employee_id, name) VALUES
    (1, 'Dana Levi'),
    (2, 'Amir Cohen'),
    (3, 'Noa Baruch');

-- client_id is SERIAL; ids are explicit here because accounts below
-- reference them, so the sequence is bumped at the end of the file.
INSERT INTO clients (client_id, name, date_of_birth, rank) VALUES
    ( 1, 'Avi Mizrahi',    DATE '1984-05-14', 6),
    ( 2, 'Rina Peretz',    DATE '1991-11-02', 4),
    ( 3, 'Yossi Alon',     DATE '1978-01-23', 8),
    ( 4, 'Maya Sharon',    DATE '1995-07-30', 3),
    ( 5, 'Eli Navon',      DATE '1988-09-09', 5),
    ( 6, 'Tamar Golan',    DATE '1979-03-18', 10),
    ( 7, 'Oded Barak',     DATE '1969-12-05', 7),
    ( 8, 'Shira Katz',     DATE '1982-06-21', 9),
    ( 9, 'Gadi Rozen',     DATE '1975-04-11', 6),
    (10, 'Lior Ashkenazi', DATE '1990-02-17', 5),
    (11, 'Hila Segev',     DATE '1986-08-25', 4),
    (12, 'Nadav Oren',     DATE '1972-10-08', 7),
    (13, 'Efrat Ziv',      DATE '1993-01-12', 2),
    (14, 'Tom Harel',      CURRENT_DATE - INTERVAL '16 years', 0),
    (15, 'Noya Gal',       CURRENT_DATE - INTERVAL '14 years', 1),
    (16, 'Itay Mor',       CURRENT_DATE - INTERVAL '12 years', 0);

INSERT INTO accounts (account_number, account_type, opening_date, bank_number,
                      balance, client_id, employee_id) VALUES
    (1000, 'REGULAR_CHECKING',  NOW(), 1,  300,  1, 1),
    (1001, 'REGULAR_CHECKING',  NOW(), 1,    0,  2, 1),
    (1002, 'REGULAR_CHECKING',  NOW(), 2,    0,  3, 2),
    (1003, 'REGULAR_CHECKING',  NOW(), 1,    0,  4, 3),
    (1004, 'REGULAR_CHECKING',  NOW(), 2,    0,  5, 2),
    (1005, 'BUSINESS_CHECKING', NOW(), 1, 1000,  6, 2),
    (1006, 'BUSINESS_CHECKING', NOW(), 2, -800,  7, 3),
    (1007, 'BUSINESS_CHECKING', NOW(), 1,    0,  8, 1),
    (1008, 'BUSINESS_CHECKING', NOW(), 2,    0,  9, 2),
    (1009, 'SAVINGS',           NOW(), 1,  200, 10, 3),
    (1010, 'SAVINGS',           NOW(), 2,    0, 11, 1),
    (1011, 'SAVINGS',           NOW(), 1,    0, 12, 3),
    (1012, 'SAVINGS',           NOW(), 2,    0, 13, 1),
    (1013, 'YOUTH',             NOW(), 1,   20, 14, 1),
    (1014, 'YOUTH',             NOW(), 2,    0, 15, 2),
    (1015, 'YOUTH',             NOW(), 1,    0, 16, 3);

-- Class Table Inheritance details rows (youth accounts have none).
INSERT INTO checking_account_details (account_number, credit_limit) VALUES
    (1000, 5000),
    (1001, 3000),
    (1002, 8000),
    (1003, 2000),
    (1004, 4500);

INSERT INTO business_account_details (account_number, credit_limit,
                                      business_revenue) VALUES
    (1005, 20000, 12000000),   -- revenue over 10M: VIP
    (1006, 15000,  2500000),
    (1007, 10000,  6000000),
    (1008, 12000,   900000);

INSERT INTO savings_account_details (account_number, deposit_amount, years) VALUES
    (1009, 25000,  5),
    (1010, 40000, 10),
    (1011, 60000,  8),
    (1012, 15000,  3);

-- Credit products: checking accounts only; 1006 carries a loan AND a mortgage.
INSERT INTO loans (account_number, original_amount, years, monthly_payment,
                   start_date) VALUES
    (1001,  30000,  5,  620, CURRENT_DATE),
    (1006, 250000, 10, 2400, CURRENT_DATE);

INSERT INTO mortgages (account_number, original_amount, years, monthly_payment,
                       start_date) VALUES
    (1002,  900000, 25, 4100, CURRENT_DATE),
    (1006, 1500000, 20, 8300, CURRENT_DATE);

-- Cards: checking unlimited; savings at most one; youth one, debit only.
-- Debit cards have no credit limit (NULL).
INSERT INTO cards (account_number, card_type, credit_limit, issued_date) VALUES
    (1000, 'DEBIT',  NULL,  CURRENT_DATE),
    (1000, 'CREDIT', 10000, CURRENT_DATE),
    (1001, 'DEBIT',  NULL,  CURRENT_DATE),
    (1002, 'CREDIT', 20000, CURRENT_DATE),
    (1003, 'DEBIT',  NULL,  CURRENT_DATE),
    (1005, 'DEBIT',  NULL,  CURRENT_DATE),
    (1005, 'CREDIT', 50000, CURRENT_DATE),
    (1006, 'CREDIT', 30000, CURRENT_DATE),
    (1009, 'DEBIT',  NULL,  CURRENT_DATE),
    (1010, 'CREDIT', 5000,  CURRENT_DATE),
    (1013, 'DEBIT',  NULL,  CURRENT_DATE),
    (1014, 'DEBIT',  NULL,  CURRENT_DATE),
    (1015, 'DEBIT',  NULL,  CURRENT_DATE);

-- The movement log behind the balances above.
INSERT INTO transactions (account_number, type, amount, occurred_at) VALUES
    (1000, 'DEPOSIT',    500, NOW()),
    (1000, 'WITHDRAWAL', 200, NOW()),
    (1005, 'DEPOSIT',   1000, NOW()),
    (1006, 'WITHDRAWAL', 800, NOW()),   -- into overdraft: fine for checking
    (1009, 'DEPOSIT',    300, NOW()),
    (1009, 'WITHDRAWAL', 100, NOW()),
    (1013, 'DEPOSIT',     50, NOW()),
    (1013, 'WITHDRAWAL',  30, NOW());

-- clients got explicit ids, so its sequence must catch up or the next
-- client created through the program would collide on client_id 1.
SELECT setval('clients_client_id_seq', 16);

COMMIT;
