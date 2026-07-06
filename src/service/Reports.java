package service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import accounts.Account;
import accounts.CheckingAccount;
import dao.AccountDAO;
import interfaces.ManagementFeeChargeable;
import interfaces.Profitable;

/**
 * The read-only side of the service layer: listings, profit and fee
 * queries. Never writes - every method loads accounts through the
 * {@code dao} layer and computes in Java, filtering purely on the
 * capability interfaces ({@link Profitable},
 * {@link ManagementFeeChargeable}) or a shared base type, never a concrete
 * account class - so a new account type changes nothing here.
 *
 * State-changing operations live in {@link BankManager}, which hands out
 * this class via {@code reports()}.
 */
public final class Reports {

    private final AccountDAO accountDao;

    Reports(AccountDAO accountDao) {
        this.accountDao = accountDao;
    }

    // ----------------------------------------------------------- listings

    /** @return every account, ascending by account number. */
    public Account[] getAllAccountsSortedByNumber() throws SQLException {
        Account[] result = accountDao.loadAll().toArray(new Account[0]);
        Arrays.sort(result);
        return result;
    }

    /**
     * @return the {@link Profitable} accounts, descending by annual profit.
     *         Savings and youth accounts never appear here - they simply
     *         don't implement the interface this method filters on.
     */
    public Account[] getProfitableAccountsSortedByProfitDesc() throws SQLException {
        List<Account> profitable = new ArrayList<>();
        for (Account account : accountDao.loadAll()) {
            if (account instanceof Profitable) {
                profitable.add(account);
            }
        }
        profitable.sort(new Comparator<Account>() {
            @Override
            public int compare(Account a, Account b) {
                double pa = ((Profitable) a).calculateAnnualProfit();
                double pb = ((Profitable) b).calculateAnnualProfit();
                return Double.compare(pb, pa);
            }
        });
        return profitable.toArray(new Account[0]);
    }

    /**
     * @param type the account class (or interface-implementing class) to match.
     * @return matching accounts, ascending by number.
     */
    public Account[] getAccountsOfType(Class<? extends Account> type) throws SQLException {
        List<Account> matches = new ArrayList<>();
        for (Account account : accountDao.loadAll()) {
            if (type.isInstance(account)) {
                matches.add(account);
            }
        }
        Account[] result = matches.toArray(new Account[0]);
        Arrays.sort(result);
        return result;
    }

    // ------------------------------------------------------------- profit

    /** @throws IllegalArgumentException if the account doesn't exist or has no profit concept. */
    public double getAnnualProfitForAccount(int number) throws SQLException {
        Account account = requireAccount(number);
        if (!(account instanceof Profitable profitable)) {
            throw new IllegalArgumentException(
                    "Account #" + number + " (" + account.getAccountTypeName()
                            + ") has no relevant annual profit.");
        }
        return profitable.calculateAnnualProfit();
    }

    /** @return the sum of annual profit across every profit-bearing account. */
    public double getTotalAnnualProfit() throws SQLException {
        double total = 0.0;
        for (Account account : accountDao.loadAll()) {
            if (account instanceof Profitable profitable) {
                total += profitable.calculateAnnualProfit();
            }
        }
        return total;
    }

    /** @return the checking account with the highest annual profit, or null if there are none. */
    public CheckingAccount getTopCheckingContributor() throws SQLException {
        CheckingAccount top = null;
        double best = 0.0;
        for (Account account : accountDao.loadAll()) {
            if (account instanceof CheckingAccount current) {
                double profit = current.calculateAnnualProfit();
                if (top == null || profit > best) {
                    best = profit;
                    top = current;
                }
            }
        }
        return top;
    }

    // ------------------------------------------------------- management fees

    /** @return the accounts that are charged a management fee, in load order. */
    public Account[] getFeeChargeableAccounts() throws SQLException {
        List<Account> chargeable = new ArrayList<>();
        for (Account account : accountDao.loadAll()) {
            if (account instanceof ManagementFeeChargeable) {
                chargeable.add(account);
            }
        }
        return chargeable.toArray(new Account[0]);
    }

    // ------------------------------------------------------------- helpers

    /** @return the account, never null. @throws IllegalArgumentException if it doesn't exist. */
    private Account requireAccount(int number) throws SQLException {
        Account account = accountDao.findByNumber(number);
        if (account == null) {
            throw new IllegalArgumentException("No account with number " + number + ".");
        }
        return account;
    }
}
