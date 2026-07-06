package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import bank.Client;

/**
 * All SQL for the {@code clients} table. Pure data access - the policy of
 * when a client is created or reused belongs to {@code BankManager}.
 */
public class ClientDAO {

    private final Connection connection;

    public ClientDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Finds the client's row by name (case-insensitive) and sets its
     * {@code clientId} and rank from the database. Inserts a new row only if
     * no client with that name exists yet. An existing client's stored rank
     * always wins over whatever rank is on the passed-in object - rank
     * changes are a deliberate separate action, not a side effect of opening
     * another account.
     */
    public void resolve(Client client) throws SQLException {
        String select = "SELECT client_id, rank FROM clients WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, client.getName());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    client.setClientId(rs.getInt("client_id"));
                    client.setRank(rs.getInt("rank"));
                    return;
                }
            }
        }
        String insert = "INSERT INTO clients (name, date_of_birth, rank) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, client.getName());
            statement.setDate(2, java.sql.Date.valueOf(client.getDateOfBirth()));
            statement.setInt(3, client.getRank());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                client.setClientId(keys.getInt(1));
            }
        }
    }
}
