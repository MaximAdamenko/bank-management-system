package menu;

import java.sql.SQLException;

import products.Transaction;
import service.BankManager;

/** The Deposits & withdrawals sub-menu. IO only. */
public final class MoneyMenu {

    private final BankManager bank;

    public MoneyMenu(BankManager bank) {
        this.bank = bank;
    }

    public void run() throws SQLException {
        while (true) {
            System.out.println(Strings.MONEY_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::deposit);
                case 2 -> ConsoleIO.attempt(this::withdraw);
                case 3 -> ConsoleIO.attempt(this::showTransactions);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    private void deposit() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        Transaction transaction = bank.deposit(number, ConsoleIO.readInt(Strings.PROMPT_AMOUNT));
        System.out.println(String.format(Strings.DONE, transaction));
    }

    private void withdraw() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        Transaction transaction = bank.withdraw(number, ConsoleIO.readInt(Strings.PROMPT_AMOUNT));
        System.out.println(String.format(Strings.DONE, transaction));
    }

    private void showTransactions() throws SQLException {
        ConsoleIO.printAll(
                bank.getTransactionsForAccount(ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER)),
                Strings.NONE_FOUND);
    }
}
