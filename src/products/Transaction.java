package products;

import java.time.LocalDateTime;

import util.Money;

/**
 * A single deposit or withdrawal on an account.
 *
 * Written by BankManager whenever an account's balance changes, so the
 * database keeps a full movement history. {@code transactionId} is 0 until
 * BankManager persists this transaction and assigns the real row id.
 */
public class Transaction {

    /** The two ways a balance can change. */
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL
    }

    private int transactionId;
    private final int accountNumber;
    private final TransactionType type;
    private final int amount;
    private final LocalDateTime occurredAt;

    public Transaction(int accountNumber, TransactionType type, int amount,
                       LocalDateTime occurredAt) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type must not be null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Transaction time must not be null.");
        }
        this.transactionId = 0;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    /** @return the database row id, or 0 if this transaction isn't persisted yet. */
    public int getTransactionId() {
        return transactionId;
    }

    /** Set once by BankManager after inserting or loading this transaction's row. */
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return type + " of " + Money.format(amount) + " on account " + accountNumber + " at " + occurredAt;
    }
}
