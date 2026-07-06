package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the bank's tables if they don't already exist.
 *
 * Class Table Inheritance: one base {@code accounts} table plus one details
 * table per concrete account type that has extra fields (youth accounts
 * have none, so the base row alone is enough). Each account belongs to
 * exactly one client. Loans, mortgages and cards are products attached to
 * an account, not accounts themselves; every deposit/withdrawal is logged
 * to {@code transactions}.
 */
public final class Schema {

    private static final String[] CREATE_STATEMENTS = {
            """
            CREATE TABLE IF NOT EXISTS employees (
                employee_id INTEGER PRIMARY KEY,
                name        VARCHAR(100) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS clients (
                client_id     SERIAL PRIMARY KEY,
                name          VARCHAR(100) NOT NULL,
                date_of_birth DATE NOT NULL,
                rank          INTEGER NOT NULL CHECK (rank BETWEEN 0 AND 10)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS accounts (
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
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS checking_account_details (
                account_number INTEGER PRIMARY KEY REFERENCES accounts (account_number)
                    ON DELETE CASCADE,
                credit_limit   DOUBLE PRECISION NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS business_account_details (
                account_number   INTEGER PRIMARY KEY REFERENCES accounts (account_number)
                    ON DELETE CASCADE,
                credit_limit     DOUBLE PRECISION NOT NULL,
                business_revenue DOUBLE PRECISION NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS savings_account_details (
                account_number  INTEGER PRIMARY KEY REFERENCES accounts (account_number)
                    ON DELETE CASCADE,
                deposit_amount  DOUBLE PRECISION NOT NULL,
                years           INTEGER NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS mortgages (
                mortgage_id     SERIAL PRIMARY KEY,
                account_number  INTEGER NOT NULL REFERENCES accounts (account_number)
                    ON DELETE RESTRICT,
                original_amount DOUBLE PRECISION NOT NULL,
                years           INTEGER NOT NULL,
                monthly_payment DOUBLE PRECISION NOT NULL,
                start_date      DATE NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS loans (
                loan_id         SERIAL PRIMARY KEY,
                account_number  INTEGER NOT NULL REFERENCES accounts (account_number)
                    ON DELETE RESTRICT,
                original_amount DOUBLE PRECISION NOT NULL,
                years           INTEGER NOT NULL,
                monthly_payment DOUBLE PRECISION NOT NULL,
                start_date      DATE NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS cards (
                card_id        SERIAL PRIMARY KEY,
                account_number INTEGER NOT NULL REFERENCES accounts (account_number)
                    ON DELETE CASCADE,
                card_type      VARCHAR(10) NOT NULL CHECK (card_type IN ('DEBIT', 'CREDIT')),
                credit_limit   DOUBLE PRECISION,
                issued_date    DATE NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id SERIAL PRIMARY KEY,
                account_number INTEGER NOT NULL REFERENCES accounts (account_number)
                    ON DELETE CASCADE,
                type           VARCHAR(10) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
                amount         INTEGER NOT NULL,
                occurred_at    TIMESTAMP NOT NULL
            )
            """
    };

    private Schema() {
    }

    /** Creates every table that doesn't already exist. Safe to call on every startup. */
    public static void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String ddl : CREATE_STATEMENTS) {
                statement.execute(ddl);
            }
        }
    }
}
