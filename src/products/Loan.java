package products;

import java.time.LocalDate;

import util.Money;

/**
 * A loan attached to an eligible account.
 *
 * Not an account itself: an account either qualifies for a loan or it
 * doesn't (only regular and business checking accounts do - enforced by
 * BankManager, not here), and an eligible account can carry any number of
 * them. {@code loanId} is 0 until BankManager persists this loan and
 * assigns the real row id.
 */
public class Loan {

    private int loanId;
    private final int accountNumber;
    private final double originalAmount;
    private final int years;
    private final double monthlyPayment;
    private final LocalDate startDate;

    public Loan(int accountNumber, double originalAmount, int years,
                double monthlyPayment, LocalDate startDate) {
        if (originalAmount <= 0) {
            throw new IllegalArgumentException("Original loan amount must be positive.");
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
        this.loanId = 0;
        this.accountNumber = accountNumber;
        this.originalAmount = originalAmount;
        this.years = years;
        this.monthlyPayment = monthlyPayment;
        this.startDate = startDate;
    }

    /** @return the database row id, or 0 if this loan isn't persisted yet. */
    public int getLoanId() {
        return loanId;
    }

    /** Set once by BankManager after inserting or loading this loan's row. */
    public void setLoanId(int loanId) {
        this.loanId = loanId;
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
        return "Loan #" + loanId + " on account " + accountNumber
                + ": " + Money.format(originalAmount) + " over " + years + " years, "
                + Money.format(monthlyPayment) + "/month, started " + startDate;
    }
}
