package util;

import java.util.Locale;

/**
 * Formats monetary amounts for display: thousands separators, always two
 * decimals, never scientific notation - {@code 12000000.0} prints as
 * {@code 12,000,000.00}. Display only; calculations keep the raw values.
 */
public final class Money {

    private Money() {
    }

    public static String format(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
