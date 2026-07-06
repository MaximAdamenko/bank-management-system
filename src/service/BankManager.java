package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import accounts.Account;
import accounts.CheckingAccount;
import accounts.YouthAccount;
import bank.Client;
import dao.AccountDAO;
import dao.ClientDAO;
import dao.EmployeeDAO;
import dao.ProductDAO;
import dao.TransactionDAO;
import db.DatabaseConnection;
import db.Schema;
import exceptions.DuplicationException;
import products.Card;
import products.Loan;
import products.Mortgage;
import products.Transaction;

/**
 * The service layer: every business rule, no SQL.
 *
 * Replaces the old in-memory {@code bank.Bank} - accounts are never cached
 * here, every operation reads and writes through the {@code dao} layer to
 * PostgreSQL, so data survives across runs. This class owns what the DAOs
 * must not: eligibility rules (who can borrow, who can hold which cards,
 * the youth age gate), account numbering policy, and transaction
 * boundaries - any operation spanning multiple tables commits or rolls
 * back as one unit here.
 *
 * Read-only reporting (listings, profit and fee queries) lives in
 * {@link Reports}, handed out via {@link #reports()}. Products are gated on
 * a shared base type, never a concrete account class: loan and mortgage
 * eligibility is "is it a checking account" - exactly regular + business -
 * so a future borrowing-capable type slots in by extending
 * {@link CheckingAccount}.
 */
public class BankManager {

    private static final int START_ACCOUNT_NUMBER = 1000;

    /** A youth account's owner must be younger than this on the opening date. */
    private static final int YOUTH_MAX_AGE = 18;

    private final String name;
    private final Connection connection;
    private final AccountDAO accountDao;
    private final ClientDAO clientDao;
    private final EmployeeDAO employeeDao;
    private final ProductDAO productDao;
    private final TransactionDAO transactionDao;
    private final Reports reports;

    public BankManager(String name) throws SQLException {
        this.name = (name == null || name.trim().isEmpty()) ? "Bank" : name.trim();
        this.connection = DatabaseConnection.getConnection();
        Schema.ensureSchema(connection);
        this.accountDao = new AccountDAO(connection);
        this.clientDao = new ClientDAO(connection);
        this.employeeDao = new EmployeeDAO(connection);
        this.productDao = new ProductDAO(connection);
        this.transactionDao = new TransactionDAO(connection);
        this.reports = new Reports(accountDao);
    }

    public String getName() {
        return name;
    }

    /** @return the read-only reporting side of the service layer. */
    public Reports reports() {
        return reports;
    }

    /** Closes the underlying database connection. Call once, on shutdown. */
    public void close() throws SQLException {
        connection.close();
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

    // ------------------------------------------------------------------ adding

    /**
     * Persists a fully-built account: the base row, its type-specific
     * details row (if any), its employee and owning client (each
     * resolved/created). All in one transaction - rolled back whole on any
     * failure.
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

        inTransaction(() -> {
            employeeDao.upsert(account.getEmployee());
            clientDao.resolve(owner);
            accountDao.insertBase(account, owner.getClientId());
            accountDao.insertDetails(account);
        });
    }

    // ------------------------------------------------------- deposits/withdrawals

    /**
     * Deposits into the account: updates the balance and logs the movement,
     * atomically.
     *
     * @return the logged transaction.
     * @throws IllegalArgumentException if the account doesn't exist or the
     *                                  amount is not positive.
     */
    public Transaction deposit(int accountNumber, int amount) throws SQLException {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);
        return applyBalanceChange(account, Transaction.TransactionType.DEPOSIT, amount);
    }

    /**
     * Withdraws from the account: updates the balance and logs the movement,
     * atomically. The account's own floor applies - 0, or the overdraft
     * limit for checking accounts.
     *
     * @return the logged transaction.
     * @throws IllegalArgumentException if the account doesn't exist, the
     *                                  amount is not positive, or the balance
     *                                  would fall below the account's floor.
     */
    public Transaction withdraw(int accountNumber, int amount) throws SQLException {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);
        return applyBalanceChange(account, Transaction.TransactionType.WITHDRAWAL, amount);
    }

    /** Writes an already-validated balance change: balance update + movement row, one transaction. */
    private Transaction applyBalanceChange(Account account, Transaction.TransactionType type,
                                           int amount) throws SQLException {
        Transaction transaction = new Transaction(account.getAccountNumber(), type, amount,
                LocalDateTime.now());
        inTransaction(() -> {
            accountDao.updateBalance(account);
            transactionDao.insert(transaction);
        });
        return transaction;
    }

    // ----------------------------------------------------------------- products

    /**
     * Opens a loan on an eligible account, starting today. Only checking
     * accounts (regular and business) can borrow.
     *
     * @return the persisted loan, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or isn't eligible.
     */
    public Loan openLoan(int accountNumber, double originalAmount, int years,
                         double monthlyPayment) throws SQLException {
        requireBorrowingAccount(accountNumber, "a loan");
        Loan loan = new Loan(accountNumber, originalAmount, years, monthlyPayment, LocalDate.now());
        productDao.insertLoan(loan);
        return loan;
    }

    /**
     * Opens a mortgage on an eligible account, starting today. Only checking
     * accounts (regular and business) can borrow.
     *
     * @return the persisted mortgage, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or isn't eligible.
     */
    public Mortgage openMortgage(int accountNumber, double originalAmount, int years,
                                 double monthlyPayment) throws SQLException {
        requireBorrowingAccount(accountNumber, "a mortgage");
        Mortgage mortgage = new Mortgage(accountNumber, originalAmount, years, monthlyPayment,
                LocalDate.now());
        productDao.insertMortgage(mortgage);
        return mortgage;
    }

    /**
     * Issues a card against an account, dated today. Checking accounts
     * (regular and business) can hold any number of cards; savings accounts
     * at most one of either type; youth accounts at most one, debit only.
     *
     * @param creditLimit the limit for a CREDIT card; must be 0 for DEBIT.
     * @return the persisted card, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or the
     *                                  card would break its account's rules.
     */
    public Card issueCard(int accountNumber, Card.CardType cardType, double creditLimit)
            throws SQLException {
        Account account = requireAccount(accountNumber);
        if (!(account instanceof CheckingAccount)) {
            if (account instanceof YouthAccount && cardType != Card.CardType.DEBIT) {
                throw new IllegalArgumentException("A youth account can only hold a debit card.");
            }
            if (productDao.countCards(accountNumber) >= 1) {
                throw new IllegalArgumentException(
                        account.getAccountTypeName() + " accounts can hold at most one card.");
            }
        }
        Card card = new Card(accountNumber, cardType, creditLimit, LocalDate.now());
        productDao.insertCard(card);
        return card;
    }

    /** @throws IllegalArgumentException unless the account exists and can borrow. */
    private void requireBorrowingAccount(int accountNumber, String product) throws SQLException {
        Account account = requireAccount(accountNumber);
        if (!(account instanceof CheckingAccount)) {
            throw new IllegalArgumentException(
                    account.getAccountTypeName() + " accounts cannot take " + product + ".");
        }
    }

    // ------------------------------------------------------------ product listings

    /** @return the account's loans, in insertion order. */
    public Loan[] getLoansForAccount(int accountNumber) throws SQLException {
        return productDao.loansFor(accountNumber).toArray(new Loan[0]);
    }

    /** @return the account's mortgages, in insertion order. */
    public Mortgage[] getMortgagesForAccount(int accountNumber) throws SQLException {
        return productDao.mortgagesFor(accountNumber).toArray(new Mortgage[0]);
    }

    /** @return the account's cards, in insertion order. */
    public Card[] getCardsForAccount(int accountNumber) throws SQLException {
        return productDao.cardsFor(accountNumber).toArray(new Card[0]);
    }

    /** @return the account's deposits/withdrawals, oldest first. */
    public Transaction[] getTransactionsForAccount(int accountNumber) throws SQLException {
        return transactionDao.listFor(accountNumber).toArray(new Transaction[0]);
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

    // ----------------------------------------------------------- listings

    /** @return every account, unsorted (insertion order is whatever the database returns). */
    public Account[] getAllAccounts() throws SQLException {
        return accountDao.loadAll().toArray(new Account[0]);
    }

    // -------------------------------------------------------- transaction plumbing

    /** The steps of one atomic multi-table write. */
    @FunctionalInterface
    private interface SqlWork {
        void run() throws SQLException;
    }

    /** Runs the given DAO calls as one transaction: all commit, or all roll back. */
    private void inTransaction(SqlWork work) throws SQLException {
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
