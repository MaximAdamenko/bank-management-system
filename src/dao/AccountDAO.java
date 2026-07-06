package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import accounts.Account;
import accounts.BusinessCheckingAccount;
import accounts.CheckingAccount;
import accounts.RegularCheckingAccount;
import accounts.SavingsAccount;
import accounts.YouthAccount;
import bank.Client;
import bank.Employee;

/**
 * All SQL for the {@code accounts} table and its per-type details tables.
 *
 * Reads reconstruct full domain objects in a single query: the owner,
 * employee and every details table are JOINed in, so loading N accounts
 * costs one statement, not 3N. Pure data access - business rules (account
 * numbering policy, eligibility, transaction boundaries) live in
 * {@code BankManager}, never here.
 */
public class AccountDAO {

    /**
     * One row per account, with owner, employee and the (at most one)
     * matching details row. LEFT JOINs because each account type hits
     * exactly one details table - the other columns come back NULL and are
     * simply never read for that type.
     */
    private static final String SELECT_ACCOUNTS =
            "SELECT a.account_number, a.account_type, a.opening_date, a.bank_number, a.balance, "
            + "c.client_id, c.name AS client_name, c.date_of_birth, c.rank, "
            + "e.employee_id, e.name AS employee_name, "
            + "cd.credit_limit AS checking_credit, "
            + "bd.credit_limit AS business_credit, bd.business_revenue, "
            + "sd.deposit_amount, sd.years "
            + "FROM accounts a "
            + "JOIN clients c ON c.client_id = a.client_id "
            + "JOIN employees e ON e.employee_id = a.employee_id "
            + "LEFT JOIN checking_account_details cd ON cd.account_number = a.account_number "
            + "LEFT JOIN business_account_details bd ON bd.account_number = a.account_number "
            + "LEFT JOIN savings_account_details sd ON sd.account_number = a.account_number";

    private final Connection connection;

    public AccountDAO(Connection connection) {
        this.connection = connection;
    }

    /** @return the account with this number, fully reconstructed, or null. */
    public Account findByNumber(int number) throws SQLException {
        String sql = SELECT_ACCOUNTS + " WHERE a.account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, number);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** @return every account, fully reconstructed, in whatever order the database returns. */
    public List<Account> loadAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ACCOUNTS);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapRow(rs));
            }
        }
        return accounts;
    }

    /** @return true if an account with this number exists. */
    public boolean exists(int number) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, number);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return one above the highest account number in use, or {@code start} if there are none. */
    public int nextNumber(int start) throws SQLException {
        String sql = "SELECT COALESCE(MAX(account_number), ?) + 1 AS next_number FROM accounts";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, start - 1);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt("next_number");
            }
        }
    }

    /** Inserts the base {@code accounts} row. The client must already be persisted. */
    public void insertBase(Account account, int clientId) throws SQLException {
        String sql = "INSERT INTO accounts "
                + "(account_number, account_type, opening_date, bank_number, balance, "
                + "client_id, employee_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, account.getAccountNumber());
            statement.setString(2, accountTypeOf(account));
            statement.setTimestamp(3, new Timestamp(account.getOpeningDate().getTime()));
            statement.setInt(4, account.getBankNumber());
            statement.setInt(5, account.getBalance());
            statement.setInt(6, clientId);
            statement.setInt(7, account.getEmployee().getId());
            statement.executeUpdate();
        }
    }

    /** Inserts the type-specific details row; youth accounts have none, so nothing happens. */
    public void insertDetails(Account account) throws SQLException {
        int number = account.getAccountNumber();
        if (account instanceof BusinessCheckingAccount business) {
            String sql = "INSERT INTO business_account_details "
                    + "(account_number, credit_limit, business_revenue) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, number);
                statement.setDouble(2, business.getCredit());
                statement.setDouble(3, business.getBusinessRevenue());
                statement.executeUpdate();
            }
        } else if (account instanceof CheckingAccount checking) {
            String sql = "INSERT INTO checking_account_details (account_number, credit_limit) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, number);
                statement.setDouble(2, checking.getCredit());
                statement.executeUpdate();
            }
        } else if (account instanceof SavingsAccount savings) {
            String sql = "INSERT INTO savings_account_details "
                    + "(account_number, deposit_amount, years) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, number);
                statement.setDouble(2, savings.getDepositAmount());
                statement.setInt(3, savings.getYears());
                statement.executeUpdate();
            }
        } else if (!(account instanceof YouthAccount)) {
            throw new IllegalStateException("Unknown account type: " + account.getClass());
        }
    }

    /** Overwrites the stored balance with the account's current in-memory balance. */
    public void updateBalance(Account account) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, account.getBalance());
            statement.setInt(2, account.getAccountNumber());
            statement.executeUpdate();
        }
    }

    private static String accountTypeOf(Account account) {
        if (account instanceof BusinessCheckingAccount) {
            return "BUSINESS_CHECKING";
        } else if (account instanceof RegularCheckingAccount) {
            return "REGULAR_CHECKING";
        } else if (account instanceof SavingsAccount) {
            return "SAVINGS";
        } else if (account instanceof YouthAccount) {
            return "YOUTH";
        }
        throw new IllegalStateException("Unknown account type: " + account.getClass());
    }

    private static Account mapRow(ResultSet rs) throws SQLException {
        int number = rs.getInt("account_number");
        String type = rs.getString("account_type");
        Timestamp openingDate = rs.getTimestamp("opening_date");
        int bankNumber = rs.getInt("bank_number");

        Employee employee = new Employee(rs.getInt("employee_id"), rs.getString("employee_name"));
        Client client = new Client(rs.getString("client_name"),
                rs.getDate("date_of_birth").toLocalDate(), rs.getInt("rank"));
        client.setClientId(rs.getInt("client_id"));
        Client[] clients = { client };

        Account account = switch (type) {
            case "REGULAR_CHECKING" -> new RegularCheckingAccount(number, bankNumber, employee,
                    clients, rs.getDouble("checking_credit"), openingDate);
            case "BUSINESS_CHECKING" -> new BusinessCheckingAccount(number, bankNumber, employee,
                    clients, rs.getDouble("business_credit"), rs.getDouble("business_revenue"),
                    openingDate);
            case "SAVINGS" -> new SavingsAccount(number, bankNumber, employee, clients,
                    rs.getDouble("deposit_amount"), rs.getInt("years"), openingDate);
            case "YOUTH" -> new YouthAccount(number, bankNumber, employee, clients, openingDate);
            default -> throw new IllegalStateException("Unknown account_type in database: " + type);
        };
        account.restoreBalance(rs.getInt("balance"));
        return account;
    }
}
