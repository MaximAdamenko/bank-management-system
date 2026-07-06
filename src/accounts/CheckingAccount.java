package accounts;

import java.util.Date;

import bank.Client;
import bank.Employee;
import interfaces.Profitable;

/**
 * Common base for accounts that offer an overdraft (credit) limit.
 *
 * Abstract because "checking account" isn't a concrete product on its own -
 * every one opened is either regular or business. The default profit
 * formula lives here, once, since it's shared by both subclasses (business
 * checking overrides it to add VIP handling and a fixed commission).
 */
public abstract class CheckingAccount extends Account implements Profitable {

    private final double credit;

    protected CheckingAccount(int accountNumber, int bankNumber, Employee employee,
                              Client[] initialClients, double credit) {
        this(accountNumber, bankNumber, employee, initialClients, credit, new Date());
    }

    /** Same as the primary constructor, but takes an explicit opening date - see {@link Account}. */
    protected CheckingAccount(int accountNumber, int bankNumber, Employee employee,
                              Client[] initialClients, double credit, Date openingDate) {
        super(accountNumber, bankNumber, employee, initialClients, openingDate);
        if (credit < 0) {
            throw new IllegalArgumentException("Credit limit must not be negative.");
        }
        this.credit = credit;
    }

    /** Deep-copy constructor. */
    protected CheckingAccount(CheckingAccount other) {
        super(other);
        this.credit = other.credit;
    }

    public double getCredit() {
        return credit;
    }

    /** Checking accounts may overdraft down to their credit limit. */
    @Override
    protected int minimumBalance() {
        return -(int) credit;
    }

    /**
     * Assumes the customer stays in overdraft up to the full credit limit
     * all year, so the bank's approximate profit is the limit times the
     * interest spread.
     */
    @Override
    public double calculateAnnualProfit() {
        return credit * RATE_DIFFERENCE;
    }

    @Override
    protected String typeSpecificDetails() {
        return "  Credit limit : " + credit + System.lineSeparator();
    }
}
