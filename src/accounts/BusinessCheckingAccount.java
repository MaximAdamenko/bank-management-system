package accounts;

import java.util.Date;

import bank.Client;
import bank.Employee;
import interfaces.ManagementFeeChargeable;
import util.Money;

/**
 * A business checking account.
 *
 * Carries a yearly revenue figure on top of the usual credit limit, and can
 * qualify as VIP. VIP accounts have their profit waived entirely; everyone
 * else pays the standard checking profit plus a flat yearly commission.
 * Every business account is charged a management fee, hence
 * {@link ManagementFeeChargeable}.
 */
public class BusinessCheckingAccount extends CheckingAccount implements ManagementFeeChargeable {

    private static final double VIP_REVENUE_THRESHOLD = 10_000_000.0;
    private static final int VIP_REQUIRED_RANK = 10;
    private static final double FIXED_COMMISSION = 3000.0;
    private static final double MANAGEMENT_FEE = 1000.0;

    private final double businessRevenue;

    public BusinessCheckingAccount(int accountNumber, int bankNumber, Employee employee,
                                   Client[] initialClients, double credit, double businessRevenue) {
        this(accountNumber, bankNumber, employee, initialClients, credit, businessRevenue, new Date());
    }

    /** Same as the primary constructor, but takes an explicit opening date - see {@link Account}. */
    public BusinessCheckingAccount(int accountNumber, int bankNumber, Employee employee,
                                   Client[] initialClients, double credit, double businessRevenue,
                                   Date openingDate) {
        super(accountNumber, bankNumber, employee, initialClients, credit, openingDate);
        if (businessRevenue < 0) {
            throw new IllegalArgumentException("Business revenue must not be negative.");
        }
        this.businessRevenue = businessRevenue;
    }

    /** Deep-copy constructor. */
    public BusinessCheckingAccount(BusinessCheckingAccount other) {
        super(other);
        this.businessRevenue = other.businessRevenue;
    }

    public double getBusinessRevenue() {
        return businessRevenue;
    }

    private boolean allClientsAtMaxRank() {
        for (int i = 0; i < getClientCount(); i++) {
            if (getClientAt(i).getRank() != VIP_REQUIRED_RANK) {
                return false;
            }
        }
        return true;
    }

    public boolean isVip() {
        return businessRevenue >= VIP_REVENUE_THRESHOLD && allClientsAtMaxRank();
    }

    @Override
    public double calculateAnnualProfit() {
        if (isVip()) {
            return 0.0;
        }
        return super.calculateAnnualProfit() + FIXED_COMMISSION;
    }

    @Override
    public double calculateManagementFee() {
        return MANAGEMENT_FEE;
    }

    /**
     * Recomputes this account's profit as if every client's rank were 0,
     * without touching the real account: the work happens on a deep copy.
     * The result only differs from the current profit when the account is
     * currently VIP (losing max-rank clients would end the VIP waiver).
     *
     * @return the hypothetical annual profit.
     */
    public double checkProfitVIP() {
        BusinessCheckingAccount copy = new BusinessCheckingAccount(this);
        copy.zeroAllClientRanks();
        return copy.calculateAnnualProfit();
    }

    @Override
    public String getAccountTypeName() {
        return "Business Checking";
    }

    @Override
    protected String typeSpecificDetails() {
        return super.typeSpecificDetails()
                + "  Revenue      : " + Money.format(businessRevenue)
                + (isVip() ? "  [VIP]" : "") + System.lineSeparator();
    }
}
