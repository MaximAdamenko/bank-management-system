package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import accounts.Account;
import accounts.YouthAccount;
import bank.Client;
import dao.AccountDAO;
import dao.ClientDAO;
import dao.EmployeeDAO;
import dao.ProductDAO;
import dao.TransactionDAO;
import exceptions.DuplicationException;
import products.Transaction;

/**
 * The account lifecycle: numbering, opening, closing and looking accounts
 * up. Deposits/withdrawals live in {@link MoneyService}; loans, mortgages
 * and cards in {@link ProductService}.
 */
public final class AccountService {

    private static final int START_ACCOUNT_NUMBER = 1000;

    /** A youth account's owner must be younger than this on the opening date. */
    private static final int YOUTH_MAX_AGE = 18;

    private final Connection connection;
    private final AccountDAO accountDao;
    private final ClientDAO clientDao;
    private final EmployeeDAO employeeDao;
    private final ProductDAO productDao;
    private final TransactionDAO transactionDao;

    AccountService(Connection connection, AccountDAO accountDao, ClientDAO clientDao,
                   EmployeeDAO employeeDao, ProductDAO productDao, TransactionDAO transactionDao) {
        this.connection = connection;
        this.accountDao = accountDao;
        this.clientDao = clientDao;
        this.employeeDao = employeeDao;
        this.productDao = productDao;
        this.transactionDao = transactionDao;
    }

    // ---------------------------------------------------------------- numbering

    /**
     * @return one above the highest account number currently in use, or the
     *         starting number if the bank has no accounts yet.
     */
    public int nextAutoNumber() throws SQLException {
        return accountDao.nextNumber(START_ACCOUNT_NUMBER);
    }

    /** @throws DuplicationException if an account with this number already exists. */
    public void validateUniqueNumber(int number) throws SQLException, DuplicationException {
        if (accountDao.exists(number)) {
            throw new DuplicationException(number);
        }
    }

    // ----------------------------------------------------------------- closing

    /**
     * Closes (deletes) the account: its details row, cards and transaction
     * log are removed with it by the schema's ON DELETE CASCADE rules.
     *
     * @throws IllegalArgumentException if the account doesn't exist, or
     *                                  still has open loans or mortgages
     *                                  (those must be settled first - the
     *                                  schema backs this with ON DELETE
     *                                  RESTRICT).
     */
    public void closeAccount(int accountNumber) throws SQLException {
        requireAccount(accountNumber);
        if (productDao.countOpenBorrowings(accountNumber) > 0) {
            throw new IllegalArgumentException("Account #" + accountNumber
                    + " still has open loans or mortgages and cannot be closed.");
        }
        accountDao.delete(accountNumber);
    }

    // ------------------------------------------------------------------ adding

    /**
     * Persists a fully-built account: the base row, its type-specific
     * details row (if any), its employee and owning client (each
     * resolved/created), and the opening balance logged as the account's
     * first deposit - so the stored balance always reconciles with the
     * movement log, the invariant the database's audit trigger enforces.
     * All in one transaction - rolled back whole on any failure.
     *
     * Every account belongs to exactly one client; a youth account's owner
     * must be under {@value #YOUTH_MAX_AGE} on the opening date.
     *
     * @throws DuplicationException if an account with the same number exists.
     */
    public void addAccount(Account account) throws SQLException, DuplicationException {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null.");
        }
        if (account.getClientCount() != 1) {
            throw new IllegalArgumentException("An account must have exactly one owning client.");
        }
        Client owner = account.getClients()[0];
        if (account instanceof YouthAccount
                && Period.between(owner.getDateOfBirth(), LocalDate.now()).getYears() >= YOUTH_MAX_AGE) {
            throw new IllegalArgumentException(
                    "A youth account's owner must be under " + YOUTH_MAX_AGE + ".");
        }
        validateUniqueNumber(account.getAccountNumber());

        TransactionRunner.run(connection, () -> {
            employeeDao.upsert(account.getEmployee());
            clientDao.resolve(owner);
            accountDao.insertBase(account, owner.getClientId());
            accountDao.insertDetails(account);
            if (account.getBalance() > 0) {
                transactionDao.insert(new Transaction(account.getAccountNumber(),
                        Transaction.TransactionType.DEPOSIT, account.getBalance(),
                        LocalDateTime.now()));
            }
        });
    }

    // --------------------------------------------------------------- look-ups

    /** @return the account with this number, or null if there isn't one. */
    public Account findAccountByNumber(int number) throws SQLException {
        return accountDao.findByNumber(number);
    }

    /** @return the account, never null. @throws IllegalArgumentException if it doesn't exist. */
    private Account requireAccount(int number) throws SQLException {
        Account account = accountDao.findByNumber(number);
        if (account == null) {
            throw new IllegalArgumentException("No account with number " + number + ".");
        }
        return account;
    }
}
