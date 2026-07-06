package products;

import java.time.LocalDate;

/**
 * A mortgage attached to an eligible account.
 *
 * Not an account itself: an account either qualifies for a mortgage or it
 * doesn't (only regular and business checking accounts do - enforced by
 * BankManager, not here), and an eligible account can carry any number of
 * them. {@code mortgageId} is 0 until BankManager persists this mortgage
 * and assigns the real row id.
 */
public class Mortgage {

    private int mortgageId;
    private final int accountNumber;
    private final double originalAmount;
    private final int years;
    private final double monthlyPayment;
    private final LocalDate startDate;

    public Mortgage(int accountNumber, double originalAmount, int years,
                    double monthlyPayment, LocalDate startDate) {
        if (originalAmount <= 0) {
            throw new IllegalArgumentException("Original mortgage amount must be positive.");
        }
        if (years <= 0) {
            throw new IllegalArgumentException("Number of years must be positive.");
        }
        if (monthlyPayment < 0) {
            throw new IllegalArgumentException("Monthly payment must not be negative.");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null.");
        }
        this.mortgageId = 0;
        this.accountNumber = accountNumber;
        this.originalAmount = originalAmount;
        this.years = years;
        this.monthlyPayment = monthlyPayment;
        this.startDate = startDate;
    }

    /** @return the database row id, or 0 if this mortgage isn't persisted yet. */
    public int getMortgageId() {
        return mortgageId;
    }

    /** Set once by BankManager after inserting or loading this mortgage's row. */
    public void setMortgageId(int mortgageId) {
        this.mortgageId = mortgageId;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public double getOriginalAmount() {
        return originalAmount;
    }

    public int getYears() {
        return years;
    }

    public double getMonthlyPayment() {
        return monthlyPayment;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    @Override
    public String toString() {
        return "Mortgage #" + mortgageId + " on account " + accountNumber
                + ": " + originalAmount + " over " + years + " years, "
                + monthlyPayment + "/month, started " + startDate;
    }
}
