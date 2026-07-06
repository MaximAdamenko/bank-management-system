package products;

import java.time.LocalDate;

/**
 * A payment card issued against an account.
 *
 * How many cards an account may hold, and of which types, depends on the
 * account type (youth: one debit card; savings: one card; regular and
 * business: unlimited) - enforced by BankManager, not here. {@code cardId}
 * is 0 until BankManager persists this card and assigns the real row id.
 */
public class Card {

    /** The two kinds of card the bank issues. */
    public enum CardType {
        DEBIT, CREDIT
    }

    private int cardId;
    private final int accountNumber;
    private final CardType cardType;
    private final double creditLimit;
    private final LocalDate issuedDate;

    /**
     * @param creditLimit the spending limit for a CREDIT card (must be
     *                    positive); must be 0 for a DEBIT card, which has none.
     */
    public Card(int accountNumber, CardType cardType, double creditLimit, LocalDate issuedDate) {
        if (cardType == null) {
            throw new IllegalArgumentException("Card type must not be null.");
        }
        if (cardType == CardType.CREDIT && creditLimit <= 0) {
            throw new IllegalArgumentException("A credit card must have a positive credit limit.");
        }
        if (cardType == CardType.DEBIT && creditLimit != 0) {
            throw new IllegalArgumentException("A debit card has no credit limit.");
        }
        if (issuedDate == null) {
            throw new IllegalArgumentException("Issued date must not be null.");
        }
        this.cardId = 0;
        this.accountNumber = accountNumber;
        this.cardType = cardType;
        this.creditLimit = creditLimit;
        this.issuedDate = issuedDate;
    }

    /** @return the database row id, or 0 if this card isn't persisted yet. */
    public int getCardId() {
        return cardId;
    }

    /** Set once by BankManager after inserting or loading this card's row. */
    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public CardType getCardType() {
        return cardType;
    }

    /** @return the credit limit, or 0 for a debit card. */
    public double getCreditLimit() {
        return creditLimit;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    @Override
    public String toString() {
        return cardType + " card #" + cardId + " on account " + accountNumber
                + (cardType == CardType.CREDIT ? " (limit " + creditLimit + ")" : "")
                + ", issued " + issuedDate;
    }
}
