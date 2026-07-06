package bank;

import java.time.LocalDate;

/**
 * An account owner.
 *
 * Identity is the name, compared case-insensitively - "avi" and "Avi" are the
 * same client. The rank (0..10) is set by an employee and may change over
 * time, which is why it's mutable. The date of birth is what gates youth
 * accounts (checked by BankManager, not here). {@code clientId} is 0 until
 * BankManager persists (or finds) this client in the database and assigns
 * the real row id.
 */
public class Client {

    public static final int MIN_RANK = 0;
    public static final int MAX_RANK = 10;

    private final String name;
    private final LocalDate dateOfBirth;
    private int rank;
    private int clientId;

    public Client(String name, LocalDate dateOfBirth, int rank) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Client name must not be empty.");
        }
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be a past date.");
        }
        validateRank(rank);
        this.name = name.trim();
        this.dateOfBirth = dateOfBirth;
        this.rank = rank;
        this.clientId = 0;
    }

    /** Deep-copy constructor. */
    public Client(Client other) {
        this.name = other.name;
        this.dateOfBirth = other.dateOfBirth;
        this.rank = other.rank;
        this.clientId = other.clientId;
    }

    private static void validateRank(int rank) {
        if (rank < MIN_RANK || rank > MAX_RANK) {
            throw new IllegalArgumentException(
                    "Client rank must be between " + MIN_RANK + " and " + MAX_RANK + ".");
        }
    }

    public String getName() {
        return name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        validateRank(rank);
        this.rank = rank;
    }

    /** @return the database row id, or 0 if this client isn't persisted yet. */
    public int getClientId() {
        return clientId;
    }

    /** Set once by BankManager after inserting or finding this client's row. */
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    /** Same name, ignoring case, means the same client. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Client)) {
            return false;
        }
        Client other = (Client) obj;
        return this.name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name + " [rank=" + rank + "]";
    }
}
