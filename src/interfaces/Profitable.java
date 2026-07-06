package interfaces;

/**
 * Marks an account as a source of annual profit for the bank.
 *
 * Only account types where a profit figure actually makes sense implement
 * this interface (checking accounts, mortgages). An account that does not
 * implement it - a savings account - can never be handed to a profit
 * computation by mistake, since the compiler simply won't allow it.
 */
public interface Profitable {

    /** Interest-rate spread applied across all profit estimates (10%). */
    double RATE_DIFFERENCE = 0.10;

    /** @return the estimated annual profit this account generates for the bank. */
    double calculateAnnualProfit();
}
