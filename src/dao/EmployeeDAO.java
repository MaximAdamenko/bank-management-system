package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import bank.Employee;

/**
 * All SQL for the {@code employees} table. Pure data access.
 */
public class EmployeeDAO {

    private final Connection connection;

    public EmployeeDAO(Connection connection) {
        this.connection = connection;
    }

    /** employee_id is user-chosen, not database-generated: first name entered for an id wins. */
    public void upsert(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (employee_id, name) VALUES (?, ?) "
                + "ON CONFLICT (employee_id) DO NOTHING";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employee.getId());
            statement.setString(2, employee.getName());
            statement.executeUpdate();
        }
    }
}
