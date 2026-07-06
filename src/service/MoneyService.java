package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import accounts.Account;
import dao.AccountDAO;
import dao.TransactionDAO;
import products.Transaction;

/**
 * Deposits, withdrawals and the movement history behind them - each
 * balance update and its transaction-log entry commit as one unit.
 */
public final class MoneyService {

    private final Connection connection;
    private final AccountDAO accountDao;
    private final TransactionDAO transactionDao;

    MoneyService(Connection connection, AccountDAO accountDao, TransactionDAO transactionDao) {
        this.connection = connection;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

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
        TransactionRunner.run(connection, () -> {
            accountDao.updateBalance(account);
            transactionDao.insert(transaction);
        });
        return transaction;
    }

    /** @return the account's deposits/withdrawals, oldest first. */
    public Transaction[] getTransactionsForAccount(int accountNumber) throws SQLException {
        return transactionDao.listFor(accountNumber).toArray(new Transaction[0]);
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
