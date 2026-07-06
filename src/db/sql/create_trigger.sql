-- ============================================================================
-- Bank Management System - integrity trigger (bonus)
--
-- Run once, after sql/create_tables.sql:
--     psql -d bankdb -f sql/create_trigger.sql
--
-- Guards the invariant that queries.sql #2 audits: an account's stored
-- balance always equals the net of its movement log. The Java program
-- already guarantees this at the service layer (balance update + movement
-- insert commit as one transaction), but raw SQL bypasses the program -
-- this trigger moves the guarantee into the database itself, where nothing
-- can bypass it:
--
--     UPDATE accounts SET balance = 999999 WHERE account_number = 1000;
--     -- ERROR: rejected at commit, no matching movement was logged
--
-- It is a DEFERRED CONSTRAINT TRIGGER, checked at COMMIT rather than per
-- statement, because within one transaction the balance update and the
-- movement insert happen in sequence - mid-transaction the invariant is
-- briefly false by design, and only the committed state has to reconcile.
-- ============================================================================

CREATE FUNCTION check_balance_matches_log() RETURNS trigger AS $$
DECLARE
    stored_balance INTEGER;
    logged_balance INTEGER;
BEGIN
    -- Re-read at commit time: the row may have changed again (or been
    -- deleted) after the UPDATE that queued this check.
    SELECT balance INTO stored_balance
    FROM accounts
    WHERE account_number = NEW.account_number;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    SELECT COALESCE(SUM(CASE WHEN type = 'DEPOSIT' THEN amount
                             ELSE -amount END), 0)
    INTO logged_balance
    FROM transactions
    WHERE account_number = NEW.account_number;

    IF stored_balance <> logged_balance THEN
        RAISE EXCEPTION
            'Balance change on account % rejected: stored balance % is not backed by the movement log (net %)',
            NEW.account_number, stored_balance, logged_balance;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER balance_backed_by_log
    AFTER UPDATE OF balance ON accounts
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    WHEN (OLD.balance IS DISTINCT FROM NEW.balance)
    EXECUTE FUNCTION check_balance_matches_log();
