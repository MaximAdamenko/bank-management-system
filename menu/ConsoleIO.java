package menu;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import exceptions.DuplicationException;

/**
 * Shared console plumbing for every menu: reading validated input,
 * printing result lists, and running one menu action so a business
 * rejection prints its reason and returns to the menu instead of crashing.
 */
public final class ConsoleIO {

    private static final Scanner in = new Scanner(System.in);

    private ConsoleIO() {
    }

    // ------------------------------------------------------------------ input

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(Strings.INVALID_NUMBER);
            }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(Strings.INVALID_DECIMAL);
            }
        }
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readLine(prompt));
            } catch (DateTimeParseException e) {
                System.out.println(Strings.INVALID_DATE);
            }
        }
    }

    // ----------------------------------------------------------------- output

    /** Prints every item on its own line, or the empty-message if there are none. */
    static void printAll(Object[] items, String emptyMessage) {
        if (items.length == 0) {
            System.out.println(emptyMessage);
        }
        for (Object item : items) {
            System.out.println(item);
        }
    }

    // ------------------------------------------------------------ error handling

    /** One menu action: may be refused by a business rule, never crashes the menu. */
    @FunctionalInterface
    interface Action {
        void run() throws SQLException, DuplicationException;
    }

    /** Runs an action; a rejected operation prints its reason and returns to the menu. */
    static void attempt(Action action) throws SQLException {
        try {
            action.run();
        } catch (IllegalArgumentException | DuplicationException e) {
            System.out.println(Strings.ERROR + e.getMessage());
        }
    }
}
