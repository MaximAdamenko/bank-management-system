package service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs a multi-DAO write as one atomic transaction: autocommit off, run the
 * work, commit; roll back (and always restore autocommit) on failure.
 * Shared by every service whose operation spans more than one DAO call.
 */
final class TransactionRunner {

    private TransactionRunner() {
    }

    /** The steps of one atomic multi-table write. */
    @FunctionalInterface
    interface SqlWork {
        void run() throws SQLException;
    }

    static void run(Connection connection, SqlWork work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            work.run();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
