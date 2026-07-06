package menu;

import java.sql.SQLException;

import accounts.Account;
import accounts.BusinessCheckingAccount;
import accounts.RegularCheckingAccount;
import accounts.SavingsAccount;
import accounts.YouthAccount;
import bank.Client;
import bank.Employee;
import exceptions.DuplicationException;
import service.BankManager;

/** The Accounts sub-menu: open, find, and list accounts. IO only. */
public final class AccountsMenu {

    private final BankManager bank;

    public AccountsMenu(BankManager bank) {
        this.bank = bank;
    }

    public void run() throws SQLException {
        while (true) {
            System.out.println(Strings.ACCOUNTS_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::openAccount);
                case 2 -> ConsoleIO.attempt(this::findAccount);
                case 3 -> ConsoleIO.attempt(this::showAllAccounts);
                case 4 -> ConsoleIO.attempt(this::showAccountsByType);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    private void openAccount() throws SQLException, DuplicationException {
        System.out.println(Strings.ACCOUNT_TYPE_MENU);
        int type = ConsoleIO.readInt(Strings.PROMPT_CHOICE);
        if (type < 1 || type > 4) {
            System.out.println(Strings.INVALID_CHOICE);
            return;
        }
        int number = ConsoleIO.readInt(Strings.PROMPT_NEW_ACCOUNT_NUMBER);
        if (number == 0) {
            number = bank.nextAutoNumber();
        }
        int bankNumber = ConsoleIO.readInt(Strings.PROMPT_BANK_NUMBER);
        Employee employee = new Employee(ConsoleIO.readInt(Strings.PROMPT_EMPLOYEE_ID),
                ConsoleIO.readLine(Strings.PROMPT_EMPLOYEE_NAME));
        Client[] owner = { new Client(ConsoleIO.readLine(Strings.PROMPT_CLIENT_NAME),
                ConsoleIO.readDate(Strings.PROMPT_CLIENT_DOB),
                ConsoleIO.readInt(Strings.PROMPT_CLIENT_RANK)) };

        Account account = switch (type) {
            case 1 -> new RegularCheckingAccount(number, bankNumber, employee, owner,
                    ConsoleIO.readDouble(Strings.PROMPT_CREDIT_LIMIT));
            case 2 -> new BusinessCheckingAccount(number, bankNumber, employee, owner,
                    ConsoleIO.readDouble(Strings.PROMPT_CREDIT_LIMIT),
                    ConsoleIO.readDouble(Strings.PROMPT_BUSINESS_REVENUE));
            case 3 -> new SavingsAccount(number, bankNumber, employee, owner,
                    ConsoleIO.readDouble(Strings.PROMPT_DEPOSIT_SUM),
                    ConsoleIO.readInt(Strings.PROMPT_YEARS));
            default -> new YouthAccount(number, bankNumber, employee, owner);
        };
        bank.addAccount(account);
        System.out.println(String.format(Strings.ACCOUNT_OPENED, number));
    }

    private void findAccount() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        Account account = bank.findAccountByNumber(number);
        if (account == null) {
            System.out.println(String.format(Strings.ACCOUNT_NOT_FOUND, number));
        } else {
            System.out.println(account);
        }
    }

    private void showAllAccounts() throws SQLException {
        ConsoleIO.printAll(bank.reports().getAllAccountsSortedByNumber(), Strings.NO_ACCOUNTS);
    }

    private void showAccountsByType() throws SQLException {
        System.out.println(Strings.ACCOUNT_TYPE_MENU);
        Class<? extends Account> type = switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
            case 1 -> RegularCheckingAccount.class;
            case 2 -> BusinessCheckingAccount.class;
            case 3 -> SavingsAccount.class;
            case 4 -> YouthAccount.class;
            default -> null;
        };
        if (type == null) {
            System.out.println(Strings.INVALID_CHOICE);
            return;
        }
        ConsoleIO.printAll(bank.reports().getAccountsOfType(type), Strings.NONE_FOUND);
    }
}
