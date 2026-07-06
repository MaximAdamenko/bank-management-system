package service;

import java.sql.Connection;
import java.sql.SQLException;

import dao.AccountDAO;
import dao.ClientDAO;
import dao.EmployeeDAO;
import dao.ProductDAO;
import dao.TransactionDAO;
import db.DatabaseConnection;

/**
 * The composition root: opens the database connection, builds the DAOs
 * once, and hands out the four single-purpose services that share them -
 * {@link AccountService} (numbering, opening, closing, look-ups),
 * {@link MoneyService} (deposits/withdrawals/history), {@link ProductService}
 * (loans, mortgages, cards) and {@link Reports} (read-only profit/fee
 * queries). Owns no business rule itself - each service can be handed to
 * only the code that needs it, instead of every caller seeing the full
 * surface of the bank.
 */
public final class BankManager {

    private final String name;
    private final Connection connection;
    private final AccountService accountService;
    private final MoneyService moneyService;
    private final ProductService productService;
    private final Reports reports;

    public BankManager(String name) throws SQLException {
        this.name = (name == null || name.trim().isEmpty()) ? "Bank" : name.trim();
        this.connection = DatabaseConnection.getConnection();

        AccountDAO accountDao = new AccountDAO(connection);
        ClientDAO clientDao = new ClientDAO(connection);
        EmployeeDAO employeeDao = new EmployeeDAO(connection);
        ProductDAO productDao = new ProductDAO(connection);
        TransactionDAO transactionDao = new TransactionDAO(connection);

        this.accountService = new AccountService(connection, accountDao, clientDao, employeeDao,
                productDao, transactionDao);
        this.moneyService = new MoneyService(connection, accountDao, transactionDao);
        this.productService = new ProductService(accountDao, productDao);
        this.reports = new Reports(accountDao);
    }

    public String getName() {
        return name;
    }

    /** @return the account lifecycle service: numbering, opening, closing, look-ups. */
    public AccountService accounts() {
        return accountService;
    }

    /** @return the deposits/withdrawals/history service. */
    public MoneyService money() {
        return moneyService;
    }

    /** @return the loans/mortgages/cards service. */
    public ProductService products() {
        return productService;
    }

    /** @return the read-only reporting side of the service layer. */
    public Reports reports() {
        return reports;
    }

    /** Closes the underlying database connection. Call once, on shutdown. */
    public void close() throws SQLException {
        connection.close();
    }
}
