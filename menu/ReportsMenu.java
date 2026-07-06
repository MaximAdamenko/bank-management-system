package menu;

import java.sql.SQLException;

import accounts.Account;
import accounts.CheckingAccount;
import interfaces.ManagementFeeChargeable;
import service.BankManager;

/** The Reports sub-menu: profit and management-fee queries. IO only. */
public final class ReportsMenu {

    private final BankManager bank;

    public ReportsMenu(BankManager bank) {
        this.bank = bank;
    }

    public void run() throws SQLException {
        while (true) {
            System.out.println(Strings.REPORTS_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::profitForAccount);
                case 2 -> ConsoleIO.attempt(this::totalProfit);
                case 3 -> ConsoleIO.attempt(this::profitableAccounts);
                case 4 -> ConsoleIO.attempt(this::topContributor);
                case 5 -> ConsoleIO.attempt(this::managementFees);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    private void profitForAccount() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        double profit = bank.reports().getAnnualProfitForAccount(number);
        System.out.println(String.format(Strings.ACCOUNT_PROFIT, number, profit));
    }

    private void totalProfit() throws SQLException {
        System.out.println(
                String.format(Strings.TOTAL_PROFIT, bank.reports().getTotalAnnualProfit()));
    }

    private void profitableAccounts() throws SQLException {
        ConsoleIO.printAll(bank.reports().getProfitableAccountsSortedByProfitDesc(),
                Strings.NONE_FOUND);
    }

    private void topContributor() throws SQLException {
        CheckingAccount top = bank.reports().getTopCheckingContributor();
        if (top == null) {
            System.out.println(Strings.NO_CHECKING_ACCOUNTS);
        } else {
            System.out.println(String.format(Strings.TOP_CONTRIBUTOR, top));
        }
    }

    /** Every fee-charged account plus the CEO's total bonus (the sum of all fees). */
    private void managementFees() throws SQLException {
        Account[] accounts = bank.reports().getFeeChargeableAccounts();
        System.out.println(Strings.FEES_HEADER);
        if (accounts.length == 0) {
            System.out.println(Strings.NO_FEES);
        }
        double ceoBonus = 0.0;
        for (Account account : accounts) {
            double fee = ((ManagementFeeChargeable) account).calculateManagementFee();
            System.out.println(String.format(Strings.FEE_LINE,
                    account.getAccountNumber(), account.getAccountTypeName(), fee));
            ceoBonus += fee;
        }
        System.out.println(String.format(Strings.CEO_BONUS, ceoBonus));
    }
}
