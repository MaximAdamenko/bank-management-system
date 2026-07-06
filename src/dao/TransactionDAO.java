package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import products.Transaction;

/**
 * All SQL for the {@code transactions} table. Pure data access - pairing a
 * movement with its balance update (atomically) is {@code BankManager}'s
 * job.
 */
public class TransactionDAO {

    private final Connection connection;

    public TransactionDAO(Connection connection) {
        this.connection = connection;
    }

    /** Inserts the movement and sets its generated id. */
    public void insert(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, type, amount, occurred_at) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, transaction.getAccountNumber());
            statement.setString(2, transaction.getType().name());
            statement.setInt(3, transaction.getAmount());
            statement.setTimestamp(4, Timestamp.valueOf(transaction.getOccurredAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                transaction.setTransactionId(keys.getInt(1));
            }
        }
    }

    /** @return the account's deposits/withdrawals, oldest first. */
    public List<Transaction> listFor(int accountNumber) throws SQLException {
        String sql = "SELECT transaction_id, type, amount, occurred_at "
                + "FROM transactions WHERE account_number = ? ORDER BY transaction_id";
        List<Transaction> transactions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction(accountNumber,
                            Transaction.TransactionType.valueOf(rs.getString("type")),
                            rs.getInt("amount"), rs.getTimestamp("occurred_at").toLocalDateTime());
                    transaction.setTransactionId(rs.getInt("transaction_id"));
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }
}
