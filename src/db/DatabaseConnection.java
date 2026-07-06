package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to the bank's PostgreSQL database.
 *
 * Credentials come from {@code db.properties} (checked-in template:
 * {@code db.properties.example}), never hardcoded. Every caller gets its own
 * {@link Connection} - this class does no pooling or caching.
 */
public final class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private DatabaseConnection() {
    }

    /** @return a new open connection built from db.properties. */
    public static Connection getConnection() throws SQLException {
        Properties props = loadProperties();
        String url = requireProperty(props, "db.url");
        String user = requireProperty(props, "db.user");
        String password = requireProperty(props, "db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadProperties() throws SQLException {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new SQLException("Could not find " + PROPERTIES_FILE
                        + " next to DatabaseConnection.class (copy db.properties.example to db.properties).");
            }
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Could not read " + PROPERTIES_FILE, e);
        }
        return props;
    }

    private static String requireProperty(Properties props, String key) throws SQLException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("Missing required property '" + key + "' in " + PROPERTIES_FILE);
        }
        return value.trim();
    }
}
