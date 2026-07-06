package accounts;

import java.util.Date;

import bank.Client;
import bank.Employee;
import util.Money;

/**
 * A savings account.
 *
 * Holds a deposit amount and a term (years). The bank neither profits nor
 * loses on these directly, so this class implements neither {@code Profitable}
 * nor {@code ManagementFeeChargeable} - it simply can't be passed to profit
 * or fee operations.
 */
public class SavingsAccount extends Account {

    private final double depositAmount;
    private final int years;

    public SavingsAccount(int accountNumber, int bankNumber, Employee employee,
                          Client[] initialClients, double depositAmount, int years) {
        this(accountNumber, bankNumber, employee, initialClients, depositAmount, years, new Date());
    }

    /** Same as the primary constructor, but takes an explicit opening date - see {@link Account}. */
    public SavingsAccount(int accountNumber, int bankNumber, Employee employee,
                          Client[] initialClients, double depositAmount, int years, Date openingDate) {
        super(accountNumber, bankNumber, employee, initialClients, openingDate);
        if (depositAmount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        if (years <= 0) {
            throw new IllegalArgumentException("Number of years must be positive.");
        }
        this.depositAmount = depositAmount;
        this.years = years;
    }

    /** Deep-copy constructor. */
    public SavingsAccount(SavingsAccount other) {
        super(other);
        this.depositAmount = other.depositAmount;
        this.years = other.years;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public int getYears() {
        return years;
    }

    @Override
    public String getAccountTypeName() {
        return "Savings";
    }

    @Override
    protected String typeSpecificDetails() {
        return "  Deposit sum  : " + Money.format(depositAmount) + System.lineSeparator()
                + "  Years        : " + years + System.lineSeparator();
    }
}
