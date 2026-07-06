import java.sql.SQLException;
import java.time.LocalDate;

import accounts.BusinessCheckingAccount;
import accounts.RegularCheckingAccount;
import accounts.SavingsAccount;
import accounts.YouthAccount;
import bank.Client;
import bank.Employee;
import exceptions.DuplicationException;
import products.Card;
import service.BankManager;

/**
 * Seeds the database with sample data, once.
 *
 * Runs only when the bank has no accounts at all; on every later start it
 * returns without touching anything (the data itself persists across runs -
 * that's PostgreSQL's job, not this class's). Deliberately contains no SQL:
 * every account, product and movement goes through {@link BankManager}'s
 * public methods - exactly the same code path as manual entry through the
 * menu - so the seed exercises the real system end to end.
 */
public final class sample {

    private sample() {
    }

    /**
     * Seeds 16 sample accounts (with their employees, owners, loans,
     * mortgages, cards and a few deposits/withdrawals) if the database is
     * empty; does nothing otherwise.
     */
    public static void seedIfEmpty(BankManager bank) throws SQLException, DuplicationException {
        if (bank.getAllAccounts().length > 0) {
            return;
        }

        Employee dana = new Employee(1, "Dana Levi");
        Employee amir = new Employee(2, "Amir Cohen");
        Employee noa = new Employee(3, "Noa Baruch");

        // --- 5 regular checking accounts -----------------------------------
        bank.addAccount(new RegularCheckingAccount(1000, 1, dana,
                owner("Avi Mizrahi", LocalDate.of(1984, 5, 14), 6), 5000));
        bank.addAccount(new RegularCheckingAccount(1001, 1, dana,
                owner("Rina Peretz", LocalDate.of(1991, 11, 2), 4), 3000));
        bank.addAccount(new RegularCheckingAccount(1002, 2, amir,
                owner("Yossi Alon", LocalDate.of(1978, 1, 23), 8), 8000));
        bank.addAccount(new RegularCheckingAccount(1003, 1, noa,
                owner("Maya Sharon", LocalDate.of(1995, 7, 30), 3), 2000));
        bank.addAccount(new RegularCheckingAccount(1004, 2, amir,
                owner("Eli Navon", LocalDate.of(1988, 9, 9), 5), 4500));

        // --- 4 business checking accounts (1005 qualifies as VIP) ----------
        bank.addAccount(new BusinessCheckingAccount(1005, 1, amir,
                owner("Tamar Golan", LocalDate.of(1979, 3, 18), 10), 20000, 12_000_000));
        bank.addAccount(new BusinessCheckingAccount(1006, 2, noa,
                owner("Oded Barak", LocalDate.of(1969, 12, 5), 7), 15000, 2_500_000));
        bank.addAccount(new BusinessCheckingAccount(1007, 1, dana,
                owner("Shira Katz", LocalDate.of(1982, 6, 21), 9), 10000, 6_000_000));
        bank.addAccount(new BusinessCheckingAccount(1008, 2, amir,
                owner("Gadi Rozen", LocalDate.of(1975, 4, 11), 6), 12000, 900_000));

        // --- 4 savings accounts --------------------------------------------
        bank.addAccount(new SavingsAccount(1009, 1, noa,
                owner("Lior Ashkenazi", LocalDate.of(1990, 2, 17), 5), 25000, 5));
        bank.addAccount(new SavingsAccount(1010, 2, dana,
                owner("Hila Segev", LocalDate.of(1986, 8, 25), 4), 40000, 10));
        bank.addAccount(new SavingsAccount(1011, 1, noa,
                owner("Nadav Oren", LocalDate.of(1972, 10, 8), 7), 60000, 8));
        bank.addAccount(new SavingsAccount(1012, 2, dana,
                owner("Efrat Ziv", LocalDate.of(1993, 1, 12), 2), 15000, 3));

        // --- 3 youth accounts (relative birth dates, so the under-18 gate
        // --- keeps holding no matter what year the seed runs) --------------
        bank.addAccount(new YouthAccount(1013, 1, dana,
                owner("Tom Harel", LocalDate.now().minusYears(16), 0)));
        bank.addAccount(new YouthAccount(1014, 2, amir,
                owner("Noya Gal", LocalDate.now().minusYears(14), 1)));
        bank.addAccount(new YouthAccount(1015, 1, noa,
                owner("Itay Mor", LocalDate.now().minusYears(12), 0)));

        // --- loans and mortgages (checking accounts only; 1006 carries both)
        bank.openLoan(1001, 30_000, 5, 620);
        bank.openLoan(1006, 250_000, 10, 2_400);
        bank.openMortgage(1002, 900_000, 25, 4_100);
        bank.openMortgage(1006, 1_500_000, 20, 8_300);

        // --- cards (checking: any number; savings: one; youth: one, debit) -
        bank.issueCard(1000, Card.CardType.DEBIT, 0);
        bank.issueCard(1000, Card.CardType.CREDIT, 10_000);
        bank.issueCard(1001, Card.CardType.DEBIT, 0);
        bank.issueCard(1002, Card.CardType.CREDIT, 20_000);
        bank.issueCard(1003, Card.CardType.DEBIT, 0);
        bank.issueCard(1005, Card.CardType.DEBIT, 0);
        bank.issueCard(1005, Card.CardType.CREDIT, 50_000);
        bank.issueCard(1006, Card.CardType.CREDIT, 30_000);
        bank.issueCard(1009, Card.CardType.DEBIT, 0);
        bank.issueCard(1010, Card.CardType.CREDIT, 5_000);
        bank.issueCard(1013, Card.CardType.DEBIT, 0);
        bank.issueCard(1014, Card.CardType.DEBIT, 0);
        bank.issueCard(1015, Card.CardType.DEBIT, 0);

        // --- a few movements, so transactions isn't empty ------------------
        bank.deposit(1000, 500);
        bank.withdraw(1000, 200);
        bank.deposit(1005, 1_000);
        bank.withdraw(1006, 800);      // into overdraft - fine for checking
        bank.deposit(1009, 300);
        bank.withdraw(1009, 100);
        bank.deposit(1013, 50);
        bank.withdraw(1013, 30);
    }

    private static Client[] owner(String name, LocalDate dateOfBirth, int rank) {
        return new Client[] { new Client(name, dateOfBirth, rank) };
    }
}
