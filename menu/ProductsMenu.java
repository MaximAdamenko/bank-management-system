package menu;

import java.sql.SQLException;

import products.Card;
import products.Loan;
import products.Mortgage;
import service.BankManager;

/** The Loans, Mortgages and Cards sub-menus - one per product type. IO only. */
public final class ProductsMenu {

    private final BankManager bank;

    public ProductsMenu(BankManager bank) {
        this.bank = bank;
    }

    public void runLoans() throws SQLException {
        while (true) {
            System.out.println(Strings.LOANS_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::takeLoan);
                case 2 -> ConsoleIO.attempt(this::showLoans);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    public void runMortgages() throws SQLException {
        while (true) {
            System.out.println(Strings.MORTGAGES_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::takeMortgage);
                case 2 -> ConsoleIO.attempt(this::showMortgages);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    public void runCards() throws SQLException {
        while (true) {
            System.out.println(Strings.CARDS_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> ConsoleIO.attempt(this::issueCard);
                case 2 -> ConsoleIO.attempt(this::showCards);
                case 3 -> ConsoleIO.attempt(this::cancelCard);
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }

    // ------------------------------------------------------------------ loans

    private void takeLoan() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        Loan loan = bank.openLoan(number, ConsoleIO.readDouble(Strings.PROMPT_ORIGINAL_AMOUNT),
                ConsoleIO.readInt(Strings.PROMPT_YEARS),
                ConsoleIO.readDouble(Strings.PROMPT_MONTHLY_PAYMENT));
        System.out.println(String.format(Strings.DONE, loan));
    }

    private void showLoans() throws SQLException {
        ConsoleIO.printAll(
                bank.getLoansForAccount(ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER)),
                Strings.NONE_FOUND);
    }

    // -------------------------------------------------------------- mortgages

    private void takeMortgage() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        Mortgage mortgage = bank.openMortgage(number,
                ConsoleIO.readDouble(Strings.PROMPT_ORIGINAL_AMOUNT),
                ConsoleIO.readInt(Strings.PROMPT_YEARS),
                ConsoleIO.readDouble(Strings.PROMPT_MONTHLY_PAYMENT));
        System.out.println(String.format(Strings.DONE, mortgage));
    }

    private void showMortgages() throws SQLException {
        ConsoleIO.printAll(
                bank.getMortgagesForAccount(ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER)),
                Strings.NONE_FOUND);
    }

    // ------------------------------------------------------------------ cards

    private void issueCard() throws SQLException {
        int number = ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER);
        System.out.println(Strings.CARD_TYPE_MENU);
        Card card = switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
            case 1 -> bank.issueCard(number, Card.CardType.DEBIT, 0);
            case 2 -> bank.issueCard(number, Card.CardType.CREDIT,
                    ConsoleIO.readDouble(Strings.PROMPT_CARD_CREDIT_LIMIT));
            default -> null;
        };
        if (card == null) {
            System.out.println(Strings.INVALID_CHOICE);
        } else {
            System.out.println(String.format(Strings.DONE, card));
        }
    }

    private void cancelCard() throws SQLException {
        int cardId = ConsoleIO.readInt(Strings.PROMPT_CARD_ID);
        bank.cancelCard(cardId);
        System.out.println(String.format(Strings.CARD_CANCELLED, cardId));
    }

    private void showCards() throws SQLException {
        ConsoleIO.printAll(
                bank.getCardsForAccount(ConsoleIO.readInt(Strings.PROMPT_ACCOUNT_NUMBER)),
                Strings.NONE_FOUND);
    }
}
