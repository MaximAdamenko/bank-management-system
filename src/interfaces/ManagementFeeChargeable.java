package interfaces;

/**
 * Marks an account as being charged an annual management fee.
 *
 * Business checking accounts and mortgages implement this; regular checking
 * accounts deliberately don't, so any code that hands out management fees can
 * work purely through this interface and never needs to know the concrete
 * account types involved.
 */
public interface ManagementFeeChargeable {

    /**
     * @return the annual management fee charged on this account. Collected
     *         fees fund the CEO's bonus and are separate from bank profit.
     */
    double calculateManagementFee();
}
