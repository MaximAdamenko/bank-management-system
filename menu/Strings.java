package menu;

/**
 * Every piece of console text in one place - Main prints nothing that isn't
 * defined here, so wording changes never touch program logic.
 */
public final class Strings {

    private Strings() {
    }

    // ------------------------------------------------------------ startup/exit

    public static final String BANK_NAME = "BankManager";
    public static final String WELCOME = "=== Welcome to %s ===";
    public static final String SEEDING = "Empty database - loading sample data...";
    public static final String SEEDED = "Sample data loaded.";
    public static final String GOODBYE = "Goodbye.";
    public static final String FATAL_DB_ERROR = "Database error: ";

    // ----------------------------------------------------------------- menus

    public static final String MAIN_MENU = """

            ===== Main menu =====
             1. Accounts
             2. Deposits & withdrawals
             3. Loans
             4. Mortgages
             5. Cards
             6. Reports
             0. Exit""";

    public static final String ACCOUNTS_MENU = """

            --- Accounts ---
             1. Open a new account
             2. Find an account by number
             3. Show all accounts
             4. Show accounts by type
             0. Back""";

    public static final String MONEY_MENU = """

            --- Deposits & withdrawals ---
             1. Deposit
             2. Withdraw
             3. Show an account's transactions
             0. Back""";

    public static final String LOANS_MENU = """

            --- Loans ---
             1. Take a loan
             2. Check an account's loans
             0. Back""";

    public static final String MORTGAGES_MENU = """

            --- Mortgages ---
             1. Take a mortgage
             2. Check an account's mortgages
             0. Back""";

    public static final String CARDS_MENU = """

            --- Cards ---
             1. Issue a card
             2. Check an account's cards
             0. Back""";

    public static final String REPORTS_MENU = """

            --- Reports ---
             1. Annual profit of one account
             2. Total annual profit
             3. Profitable accounts (highest first)
             4. Top checking contributor
             5. Management fees
             0. Back""";

    public static final String ACCOUNT_TYPE_MENU = """

            Account type:
             1. Regular checking
             2. Business checking
             3. Savings
             4. Youth""";

    public static final String CARD_TYPE_MENU = """

            Card type:
             1. Debit
             2. Credit""";

    // --------------------------------------------------------------- prompts

    public static final String PROMPT_CHOICE = "Choice: ";
    public static final String PROMPT_ACCOUNT_NUMBER = "Account number: ";
    public static final String PROMPT_NEW_ACCOUNT_NUMBER = "Account number (0 = automatic): ";
    public static final String PROMPT_BANK_NUMBER = "Branch number: ";
    public static final String PROMPT_EMPLOYEE_ID = "Approving employee id: ";
    public static final String PROMPT_EMPLOYEE_NAME = "Approving employee name: ";
    public static final String PROMPT_CLIENT_NAME = "Client name: ";
    public static final String PROMPT_CLIENT_DOB = "Client date of birth (YYYY-MM-DD): ";
    public static final String PROMPT_CLIENT_RANK = "Client rank (0-10): ";
    public static final String PROMPT_CREDIT_LIMIT = "Credit limit: ";
    public static final String PROMPT_BUSINESS_REVENUE = "Yearly business revenue: ";
    public static final String PROMPT_DEPOSIT_SUM = "Deposit amount: ";
    public static final String PROMPT_YEARS = "Years: ";
    public static final String PROMPT_AMOUNT = "Amount: ";
    public static final String PROMPT_ORIGINAL_AMOUNT = "Original amount: ";
    public static final String PROMPT_MONTHLY_PAYMENT = "Monthly payment: ";
    public static final String PROMPT_CARD_CREDIT_LIMIT = "Card credit limit: ";

    // -------------------------------------------------------------- feedback

    public static final String ACCOUNT_OPENED = "Account #%d opened.";
    public static final String ACCOUNT_NOT_FOUND = "No account with number %d.";
    public static final String NO_ACCOUNTS = "(no accounts)";
    public static final String NONE_FOUND = "(none)";
    public static final String DONE = "Done: %s";
    public static final String TOTAL_PROFIT = "Total annual profit: %s";
    public static final String ACCOUNT_PROFIT = "Annual profit of account #%d: %s";
    public static final String TOP_CONTRIBUTOR = "Top checking contributor:%n%s";
    public static final String NO_CHECKING_ACCOUNTS = "There are no checking accounts.";
    public static final String FEES_HEADER = "--- Annual management fees ---";
    public static final String FEE_LINE = "  Account #%d (%s): %s";
    public static final String NO_FEES = "  (no accounts are charged a management fee)";
    public static final String CEO_BONUS = "CEO annual bonus (sum of all management fees): %s";

    // ---------------------------------------------------------------- errors

    public static final String INVALID_CHOICE = "Invalid choice, try again.";
    public static final String INVALID_NUMBER = "Please enter a whole number.";
    public static final String INVALID_DECIMAL = "Please enter a number.";
    public static final String INVALID_DATE = "Please enter a date as YYYY-MM-DD.";
    public static final String ERROR = "Error: ";
}
