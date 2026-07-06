package service;

import java.sql.SQLException;
import java.time.LocalDate;

import accounts.Account;
import accounts.CheckingAccount;
import accounts.YouthAccount;
import dao.AccountDAO;
import dao.ProductDAO;
import products.Card;
import products.Loan;
import products.Mortgage;

/**
 * Loans, mortgages and cards: which accounts may hold which product, and
 * their listings. Eligibility is gated on a shared base type, never a
 * concrete account class - loan and mortgage eligibility is "is it a
 * checking account" (exactly regular + business), so a future
 * borrowing-capable type slots in by extending {@link CheckingAccount}.
 */
public final class ProductService {

    private final AccountDAO accountDao;
    private final ProductDAO productDao;

    ProductService(AccountDAO accountDao, ProductDAO productDao) {
        this.accountDao = accountDao;
        this.productDao = productDao;
    }

    /**
     * Opens a loan on an eligible account, starting today. Only checking
     * accounts (regular and business) can borrow.
     *
     * @return the persisted loan, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or isn't eligible.
     */
    public Loan openLoan(int accountNumber, double originalAmount, int years,
                         double monthlyPayment) throws SQLException {
        requireBorrowingAccount(accountNumber, "a loan");
        Loan loan = new Loan(accountNumber, originalAmount, years, monthlyPayment, LocalDate.now());
        productDao.insertLoan(loan);
        return loan;
    }

    /**
     * Opens a mortgage on an eligible account, starting today. Only checking
     * accounts (regular and business) can borrow.
     *
     * @return the persisted mortgage, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or isn't eligible.
     */
    public Mortgage openMortgage(int accountNumber, double originalAmount, int years,
                                 double monthlyPayment) throws SQLException {
        requireBorrowingAccount(accountNumber, "a mortgage");
        Mortgage mortgage = new Mortgage(accountNumber, originalAmount, years, monthlyPayment,
                LocalDate.now());
        productDao.insertMortgage(mortgage);
        return mortgage;
    }

    /**
     * Issues a card against an account, dated today. Checking accounts
     * (regular and business) can hold any number of cards; savings accounts
     * at most one of either type; youth accounts at most one, debit only.
     *
     * @param creditLimit the limit for a CREDIT card; must be 0 for DEBIT.
     * @return the persisted card, with its generated id.
     * @throws IllegalArgumentException if the account doesn't exist or the
     *                                  card would break its account's rules.
     */
    public Card issueCard(int accountNumber, Card.CardType cardType, double creditLimit)
            throws SQLException {
        Account account = requireAccount(accountNumber);
        if (!(account instanceof CheckingAccount)) {
            if (account instanceof YouthAccount && cardType != Card.CardType.DEBIT) {
                throw new IllegalArgumentException("A youth account can only hold a debit card.");
            }
            if (productDao.countCards(accountNumber) >= 1) {
                throw new IllegalArgumentException(
                        account.getAccountTypeName() + " accounts can hold at most one card.");
            }
        }
        Card card = new Card(accountNumber, cardType, creditLimit, LocalDate.now());
        productDao.insertCard(card);
        return card;
    }

    /**
     * Cancels (deletes) a card.
     *
     * @throws IllegalArgumentException if no card has this id.
     */
    public void cancelCard(int cardId) throws SQLException {
        if (!productDao.deleteCard(cardId)) {
            throw new IllegalArgumentException("No card with id " + cardId + ".");
        }
    }

    /** @return the account's loans, in insertion order. */
    public Loan[] getLoansForAccount(int accountNumber) throws SQLException {
        return productDao.loansFor(accountNumber).toArray(new Loan[0]);
    }

    /** @return the account's mortgages, in insertion order. */
    public Mortgage[] getMortgagesForAccount(int accountNumber) throws SQLException {
        return productDao.mortgagesFor(accountNumber).toArray(new Mortgage[0]);
    }

    /** @return the account's cards, in insertion order. */
    public Card[] getCardsForAccount(int accountNumber) throws SQLException {
        return productDao.cardsFor(accountNumber).toArray(new Card[0]);
    }

    /** @throws IllegalArgumentException unless the account exists and can borrow. */
    private void requireBorrowingAccount(int accountNumber, String product) throws SQLException {
        Account account = requireAccount(accountNumber);
        if (!(account instanceof CheckingAccount)) {
            throw new IllegalArgumentException(
                    account.getAccountTypeName() + " accounts cannot take " + product + ".");
        }
    }

    /** @return the account, never null. @throws IllegalArgumentException if it doesn't exist. */
    private Account requireAccount(int number) throws SQLException {
        Account account = accountDao.findByNumber(number);
        if (account == null) {
            throw new IllegalArgumentException("No account with number " + number + ".");
        }
        return account;
    }
}
