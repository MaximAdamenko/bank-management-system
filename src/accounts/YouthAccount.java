package accounts;

import java.util.Date;

import bank.Client;
import bank.Employee;

/**
 * A youth account, for clients under the age limit (checked by BankManager
 * at opening time, using the client's date of birth).
 *
 * The simplest account the bank offers: no overdraft, no loan or mortgage
 * eligibility, at most one debit card. The bank neither profits nor charges
 * fees on it, so it implements neither {@code Profitable} nor
 * {@code ManagementFeeChargeable} - it simply can't be passed to profit or
 * fee operations.
 */
public class YouthAccount extends Account {

    public YouthAccount(int accountNumber, int bankNumber, Employee employee,
                        Client[] initialClients) {
        super(accountNumber, bankNumber, employee, initialClients);
    }

    /** Same as the primary constructor, but takes an explicit opening date - see {@link Account}. */
    public YouthAccount(int accountNumber, int bankNumber, Employee employee,
                        Client[] initialClients, Date openingDate) {
        super(accountNumber, bankNumber, employee, initialClients, openingDate);
    }

    /** Deep-copy constructor. */
    public YouthAccount(YouthAccount other) {
        super(other);
    }

    @Override
    public String getAccountTypeName() {
        return "Youth";
    }

    @Override
    protected String typeSpecificDetails() {
        return "";
    }
}
