package accounts;

import java.util.Date;

import bank.Client;
import bank.Employee;

/**
 * A private checking account.
 *
 * Uses the standard checking-account profit formula as-is and carries no
 * management fee - it deliberately does not implement
 * {@code ManagementFeeChargeable}.
 */
public class RegularCheckingAccount extends CheckingAccount {

    public RegularCheckingAccount(int accountNumber, int bankNumber, Employee employee,
                                  Client[] initialClients, double credit) {
        super(accountNumber, bankNumber, employee, initialClients, credit);
    }

    /** Same as the primary constructor, but takes an explicit opening date - see {@link Account}. */
    public RegularCheckingAccount(int accountNumber, int bankNumber, Employee employee,
                                  Client[] initialClients, double credit, Date openingDate) {
        super(accountNumber, bankNumber, employee, initialClients, credit, openingDate);
    }

    /** Deep-copy constructor. */
    public RegularCheckingAccount(RegularCheckingAccount other) {
        super(other);
    }

    @Override
    public String getAccountTypeName() {
        return "Regular Checking";
    }
}
