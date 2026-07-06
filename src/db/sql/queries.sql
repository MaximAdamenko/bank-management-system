-- ============================================================================
-- Bank Management System - 12 meaningful queries
--
-- Run against a seeded database:
--     psql -d bankdb -f sql/queries.sql
--
-- Queries 1-3 are (or extend) SQL the Java program itself runs through its
-- DAO layer; the rest answer management questions directly in the database.
-- ============================================================================


-- 1. The full picture of every account: owner, approving employee, and the
--    type-specific details, in ONE statement. This is the exact shape the
--    program's AccountDAO uses to load accounts - the LEFT JOINs hit the
--    Class Table Inheritance details tables, and each account matches at
--    most one of them (youth accounts match none).
SELECT a.account_number, a.account_type, a.balance,
       c.name  AS client,   c.rank,
       e.name  AS employee,
       cd.credit_limit      AS checking_credit,
       bd.credit_limit      AS business_credit, bd.business_revenue,
       sd.deposit_amount, sd.years
FROM accounts a
JOIN clients   c ON c.client_id   = a.client_id
JOIN employees e ON e.employee_id = a.employee_id
LEFT JOIN checking_account_details cd ON cd.account_number = a.account_number
LEFT JOIN business_account_details bd ON bd.account_number = a.account_number
LEFT JOIN savings_account_details  sd ON sd.account_number = a.account_number
ORDER BY a.account_number;


-- 2. Integrity audit: accounts whose stored balance disagrees with their
--    movement log. Healthy data returns zero rows - the program updates
--    balance and inserts the movement in one database transaction, so a row
--    here would mean that atomicity was broken.
SELECT a.account_number, a.balance,
       COALESCE(SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount
                         ELSE -t.amount END), 0) AS balance_from_log
FROM accounts a
LEFT JOIN transactions t ON t.account_number = a.account_number
GROUP BY a.account_number, a.balance
HAVING a.balance <> COALESCE(SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount
                                      ELSE -t.amount END), 0);


-- 3. Money flow per active account: total deposited, total withdrawn, and
--    net movement (conditional aggregation over the movement log).
SELECT t.account_number,
       SUM(CASE WHEN t.type = 'DEPOSIT'    THEN t.amount ELSE 0 END) AS total_deposited,
       SUM(CASE WHEN t.type = 'WITHDRAWAL' THEN t.amount ELSE 0 END) AS total_withdrawn,
       SUM(CASE WHEN t.type = 'DEPOSIT'    THEN t.amount ELSE -t.amount END) AS net
FROM transactions t
GROUP BY t.account_number
ORDER BY t.account_number;


-- 4. VIP businesses: business accounts whose yearly revenue crosses the
--    10,000,000 VIP threshold, with their owners.
SELECT a.account_number, c.name AS owner, bd.business_revenue
FROM accounts a
JOIN business_account_details bd ON bd.account_number = a.account_number
JOIN clients c ON c.client_id = a.client_id
WHERE bd.business_revenue > 10000000
ORDER BY bd.business_revenue DESC;


-- 5. Youth account owners with their computed age - every one of them must
--    be under 18, the rule the program enforces at opening time.
SELECT a.account_number, c.name AS owner, c.date_of_birth,
       EXTRACT(YEAR FROM AGE(CURRENT_DATE, c.date_of_birth)) AS age
FROM accounts a
JOIN clients c ON c.client_id = a.client_id
WHERE a.account_type = 'YOUTH'
ORDER BY age DESC;


-- 6. Monthly debt burden: what each borrowing account pays per month across
--    all its loans and mortgages combined, heaviest first.
SELECT b.account_number, c.name AS owner,
       COUNT(*)               AS open_borrowings,
       SUM(b.monthly_payment) AS monthly_total
FROM (SELECT account_number, monthly_payment FROM loans
      UNION ALL
      SELECT account_number, monthly_payment FROM mortgages) b
JOIN accounts a ON a.account_number = b.account_number
JOIN clients  c ON c.client_id = a.client_id
GROUP BY b.account_number, c.name
ORDER BY monthly_total DESC;


-- 7. Employee workload: how many accounts each employee approved, busiest
--    first (employees with no accounts still appear, with 0).
SELECT e.employee_id, e.name,
       COUNT(a.account_number) AS accounts_approved
FROM employees e
LEFT JOIN accounts a ON a.employee_id = e.employee_id
GROUP BY e.employee_id, e.name
ORDER BY accounts_approved DESC, e.employee_id;


-- 8. Accounts holding more than one card (only checking accounts may -
--    savings and youth are capped at one by the program).
SELECT a.account_number, a.account_type, COUNT(*) AS cards
FROM accounts a
JOIN cards cr ON cr.account_number = a.account_number
GROUP BY a.account_number, a.account_type
HAVING COUNT(*) > 1
ORDER BY cards DESC;


-- 9. Overdrawn accounts and how much overdraft headroom they have left.
--    Only checking accounts can go negative; their limit lives in one of
--    the two CTI details tables, hence the COALESCE.
SELECT a.account_number, a.account_type, a.balance,
       COALESCE(cd.credit_limit, bd.credit_limit)             AS credit_limit,
       COALESCE(cd.credit_limit, bd.credit_limit) + a.balance AS headroom_left
FROM accounts a
LEFT JOIN checking_account_details cd ON cd.account_number = a.account_number
LEFT JOIN business_account_details bd ON bd.account_number = a.account_number
WHERE a.balance < 0
ORDER BY a.balance;


-- 10. The single largest movement on each active account (DISTINCT ON keeps
--     exactly one row per account: the first by the ORDER BY).
SELECT DISTINCT ON (t.account_number)
       t.account_number, t.type, t.amount, t.occurred_at
FROM transactions t
ORDER BY t.account_number, t.amount DESC, t.occurred_at;


-- 11. The bank's total lending exposure, by product type.
SELECT 'LOAN' AS product, COUNT(*) AS open_count,
       SUM(original_amount) AS total_lent, SUM(monthly_payment) AS monthly_income
FROM loans
UNION ALL
SELECT 'MORTGAGE', COUNT(*), SUM(original_amount), SUM(monthly_payment)
FROM mortgages
ORDER BY total_lent DESC;


-- 12. Portfolio overview: how many accounts of each type the bank holds,
--     and the average and total balance per type.
SELECT a.account_type,
       COUNT(*)                       AS accounts,
       ROUND(AVG(a.balance), 2)       AS avg_balance,
       SUM(a.balance)                 AS total_balance
FROM accounts a
GROUP BY a.account_type
ORDER BY accounts DESC, a.account_type;
