package accounts;

import java.util.Arrays;
import java.util.Date;

import bank.Client;
import bank.Employee;
import util.Money;

/**
 * Common base for every account the bank can hold.
 *
 * A bare "account" has no meaning on its own - it's always a checking,
 * savings or youth account - so this class is abstract and only ever used
 * through its concrete subclasses. It owns everything every account shares:
 * identity, balance, the approving employee and the (manually grown) array
 * of owning clients.
 *
 * Accounts are equal, and ordered, purely by account number.
 */
public abstract class Account implements Comparable<Account> {

    /** Bonus balance every new account starts with. */
    public static final int DEFAULT_BALANCE = 20;

    /** How many extra slots to add each time the clients array is full. */
    private static final int ARRAY_GROWTH = 2;

    private final int accountNumber;
    private final Date openingDate;
    private final int bankNumber;
    private int balance;
    private final Employee employee;

    private Client[] clients;
    private int clientCount;

    /**
     * @param accountNumber  a unique account number (uniqueness is checked
     *                       by the caller before this constructor runs)
     * @param bankNumber     the branch number
     * @param employee       the employee approving the account
     * @param initialClients one or more owning clients
     */
    protected Account(int accountNumber, int bankNumber, Employee employee, Client[] initialClients) {
        this(accountNumber, bankNumber, employee, initialClients, new Date());
    }

    /**
     * Same as the primary constructor, but takes an explicit opening date -
     * used only by BankManager when reconstructing an account already
     * persisted in the database, so the original opening date survives a
     * restart instead of being reset to now.
     */
    protected Account(int accountNumber, int bankNumber, Employee employee, Client[] initialClients,
                       Date openingDate) {
        if (bankNumber <= 0) {
            throw new IllegalArgumentException("Bank number must be a positive number.");
        }
        if (employee == null) {
            throw new IllegalArgumentException("An approving employee is required.");
        }
        if (initialClients == null || initialClients.length == 0) {
            throw new IllegalArgumentException("An account must have at least one client.");
        }
        if (openingDate == null) {
            throw new IllegalArgumentException("Opening date must not be null.");
        }

        this.accountNumber = accountNumber;
        this.bankNumber = bankNumber;
        this.employee = employee;
        this.balance = DEFAULT_BALANCE;
        this.openingDate = new Date(openingDate.getTime());

        this.clients = new Client[initialClients.length];
        this.clientCount = 0;
        for (Client client : initialClients) {
            addClient(client);
        }
        if (clientCount == 0) {
            throw new IllegalArgumentException("An account must have at least one valid client.");
        }
    }

    /**
     * Deep-copy constructor: employee, clients and opening date are all
     * independently copied, so mutating the copy never touches the
     * original. Used by {@code BusinessCheckingAccount.checkProfitVIP}.
     */
    protected Account(Account other) {
        this.accountNumber = other.accountNumber;
        this.bankNumber = other.bankNumber;
        this.balance = other.balance;
        this.openingDate = new Date(other.openingDate.getTime());
        this.employee = new Employee(other.employee);
        this.clientCount = other.clientCount;
        this.clients = new Client[other.clients.length];
        for (int i = 0; i < clientCount; i++) {
            this.clients[i] = new Client(other.clients[i]);
        }
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public Date getOpeningDate() {
        return new Date(openingDate.getTime());
    }

    public int getBankNumber() {
        return bankNumber;
    }

    public int getBalance() {
        return balance;
    }

    public Employee getEmployee() {
        return employee;
    }

    /**
     * The lowest balance this account may reach. Plain accounts stop at 0;
     * checking accounts override this to allow overdraft down to their
     * credit limit.
     */
    protected int minimumBalance() {
        return 0;
    }

    /**
     * Overwrites the balance with its persisted value. Used only by
     * BankManager when rebuilding an account from its database row, so a
     * balance changed by deposits/withdrawals survives a restart instead of
     * being reset to {@link #DEFAULT_BALANCE} by the constructor.
     */
    public void restoreBalance(int balance) {
        this.balance = balance;
    }

    /**
     * Adds the given amount to the balance. Persisting the change and
     * logging the transaction are BankManager's job, not this class's.
     *
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        balance += amount;
    }

    /**
     * Subtracts the given amount from the balance, refusing to go below
     * {@link #minimumBalance()}. Persisting the change and logging the
     * transaction are BankManager's job, not this class's.
     *
     * @throws IllegalArgumentException if the amount is not positive or
     *                                  would take the balance below the floor.
     */
    public void withdraw(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if (balance - amount < minimumBalance()) {
            throw new IllegalArgumentException(
                    "Withdrawal would take the balance below " + minimumBalance() + ".");
        }
        balance -= amount;
    }

    public int getClientCount() {
        return clientCount;
    }

    /** @return the client at the given index; used by subclasses (e.g. VIP checks). */
    protected Client getClientAt(int index) {
        return clients[index];
    }

    /** @return a defensive copy of the owning clients. */
    public Client[] getClients() {
        return Arrays.copyOf(clients, clientCount);
    }

    /**
     * Adds an owning client, rejecting duplicates (same name, ignoring case).
     *
     * @return true if added, false if the client was null or already present.
     */
    public boolean addClient(Client client) {
        if (client == null) {
            return false;
        }
        for (int i = 0; i < clientCount; i++) {
            if (clients[i].equals(client)) {
                return false;
            }
        }
        if (clientCount == clients.length) {
            clients = Arrays.copyOf(clients, clients.length + ARRAY_GROWTH);
        }
        clients[clientCount++] = client;
        return true;
    }

    /** Resets every client's rank to 0; only meant to run on a throwaway deep copy. */
    protected void zeroAllClientRanks() {
        for (int i = 0; i < clientCount; i++) {
            clients[i].setRank(0);
        }
    }

    /** @return a short, human-readable name for the concrete account type. */
    public abstract String getAccountTypeName();

    /** @return the fields specific to the concrete type, formatted for display. */
    protected abstract String typeSpecificDetails();

    private String clientsToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clientCount; i++) {
            sb.append("    - ").append(clients[i]).append(System.lineSeparator());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Account #").append(accountNumber)
          .append(" (").append(getAccountTypeName()).append(")").append(System.lineSeparator());
        sb.append("  Opening date : ").append(openingDate).append(System.lineSeparator());
        sb.append("  Bank number  : ").append(bankNumber).append(System.lineSeparator());
        sb.append("  Balance      : ").append(Money.format(balance)).append(System.lineSeparator());
        sb.append("  Employee     : ").append(employee).append(System.lineSeparator());
        sb.append(typeSpecificDetails());
        sb.append("  Clients      :").append(System.lineSeparator());
        sb.append(clientsToString());
        return sb.toString();
    }

    /** Two accounts are the same account if they share an account number. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Account)) {
            return false;
        }
        Account other = (Account) obj;
        return this.accountNumber == other.accountNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(accountNumber);
    }

    /** Ascending by account number. */
    @Override
    public int compareTo(Account other) {
        return Integer.compare(this.accountNumber, other.accountNumber);
    }
}
