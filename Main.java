import java.sql.SQLException;

import menu.AccountsMenu;
import menu.ConsoleIO;
import menu.MoneyMenu;
import menu.ProductsMenu;
import menu.ReportsMenu;
import menu.Strings;
import service.BankManager;

/**
 * The entry point: connects and runs the main menu loop - nothing else.
 * The database itself is created and filled with plain SQL, in the
 * database (src/db/sql/create_tables.sql, src/db/sql/insert_data.sql - see README).
 * Each main-menu category is its own class ({@link AccountsMenu},
 * {@link MoneyMenu}, {@link ProductsMenu}, {@link ReportsMenu}); shared
 * input/output plumbing is {@link ConsoleIO}. Every label lives in
 * {@link Strings}, every business rule and every SQL statement lives
 * behind {@link BankManager}. Bad input or a rejected operation returns
 * to the menu - only a database failure ends the program.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            BankManager bank = new BankManager(Strings.BANK_NAME);
            System.out.println(String.format(Strings.WELCOME, bank.getName()));
            mainLoop(bank);
            bank.close();
            System.out.println(Strings.GOODBYE);
        } catch (SQLException e) {
            System.out.println(Strings.FATAL_DB_ERROR + e.getMessage());
        }
    }

    private static void mainLoop(BankManager bank) throws SQLException {
        AccountsMenu accounts = new AccountsMenu(bank.accounts(), bank.reports());
        MoneyMenu money = new MoneyMenu(bank.money());
        ProductsMenu products = new ProductsMenu(bank.products());
        ReportsMenu reports = new ReportsMenu(bank.reports());
        while (true) {
            System.out.println(Strings.MAIN_MENU);
            switch (ConsoleIO.readInt(Strings.PROMPT_CHOICE)) {
                case 1 -> accounts.run();
                case 2 -> money.run();
                case 3 -> products.runLoans();
                case 4 -> products.runMortgages();
                case 5 -> products.runCards();
                case 6 -> reports.run();
                case 0 -> {
                    return;
                }
                default -> System.out.println(Strings.INVALID_CHOICE);
            }
        }
    }
}
