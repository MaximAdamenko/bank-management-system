-- ============================================================================
-- Bank Management System - schema (10 tables)
--
-- Run once against a fresh database, before starting the Java program:
--     createdb bankdb
--     psql -d bankdb -f sql/create_tables.sql
--
-- Design: Class Table Inheritance for accounts - one base "accounts" table
-- plus one details table per concrete account type that has extra fields
-- (youth accounts have none, so the base row alone is enough). Each account
-- belongs to exactly one client. Loans, mortgages and cards are products
-- attached to an account, not accounts themselves; every deposit/withdrawal
-- is logged to "transactions".
--
-- Delete rules:
--   RESTRICT - a client/employee with accounts, or an account with open
--              loans/mortgages, cannot be deleted.
--   CASCADE  - deleting an account removes its details row, its cards and
--              its transaction log.
-- ============================================================================

CREATE TABLE employees (
    employee_id INTEGER PRIMARY KEY,
    name        VARCHAR(100) NOT NULL
);

CREATE TABLE clients (
    client_id     SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    rank          INTEGER NOT NULL CHECK (rank BETWEEN 0 AND 10)
);

CREATE TABLE accounts (
    account_number INTEGER PRIMARY KEY,
    account_type   VARCHAR(20) NOT NULL
        CHECK (account_type IN ('REGULAR_CHECKING', 'BUSINESS_CHECKING',
                                 'SAVINGS', 'YOUTH')),
    opening_date   TIMESTAMP NOT NULL,
    bank_number    INTEGER NOT NULL,
    balance        INTEGER NOT NULL,
    client_id      INTEGER NOT NULL REFERENCES clients (client_id)
        ON DELETE RESTRICT,
    employee_id    INTEGER NOT NULL REFERENCES employees (employee_id)
        ON DELETE RESTRICT
);

CREATE TABLE checking_account_details (
    account_number INTEGER PRIMARY KEY REFERENCES accounts (account_number)
        ON DELETE CASCADE,
    credit_limit   DOUBLE PRECISION NOT NULL
);

CREATE TABLE business_account_details (
    account_number   INTEGER PRIMARY KEY REFERENCES accounts (account_number)
        ON DELETE CASCADE,
    credit_limit     DOUBLE PRECISION NOT NULL,
    business_revenue DOUBLE PRECISION NOT NULL
);

CREATE TABLE savings_account_details (
    account_number  INTEGER PRIMARY KEY REFERENCES accounts (account_number)
        ON DELETE CASCADE,
    deposit_amount  DOUBLE PRECISION NOT NULL,
    years           INTEGER NOT NULL
);

CREATE TABLE mortgages (
    mortgage_id     SERIAL PRIMARY KEY,
    account_number  INTEGER NOT NULL REFERENCES accounts (account_number)
        ON DELETE RESTRICT,
    original_amount DOUBLE PRECISION NOT NULL,
    years           INTEGER NOT NULL,
    monthly_payment DOUBLE PRECISION NOT NULL,
    start_date      DATE NOT NULL
);

CREATE TABLE loans (
    loan_id         SERIAL PRIMARY KEY,
    account_number  INTEGER NOT NULL REFERENCES accounts (account_number)
        ON DELETE RESTRICT,
    original_amount DOUBLE PRECISION NOT NULL,
    years           INTEGER NOT NULL,
    monthly_payment DOUBLE PRECISION NOT NULL,
    start_date      DATE NOT NULL
);

CREATE TABLE cards (
    card_id        SERIAL PRIMARY KEY,
    account_number INTEGER NOT NULL REFERENCES accounts (account_number)
        ON DELETE CASCADE,
    card_type      VARCHAR(10) NOT NULL CHECK (card_type IN ('DEBIT', 'CREDIT')),
    credit_limit   DOUBLE PRECISION,
    issued_date    DATE NOT NULL
);

CREATE TABLE transactions (
    transaction_id SERIAL PRIMARY KEY,
    account_number INTEGER NOT NULL REFERENCES accounts (account_number)
        ON DELETE CASCADE,
    type           VARCHAR(10) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
    amount         INTEGER NOT NULL,
    occurred_at    TIMESTAMP NOT NULL
);
