package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import products.Card;
import products.Loan;
import products.Mortgage;

/**
 * All SQL for the {@code loans}, {@code mortgages} and {@code cards} tables.
 * Pure data access - which accounts may hold which products is
 * {@code BankManager}'s rule, never checked here.
 */
public class ProductDAO {

    private final Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    // ------------------------------------------------------------------- loans

    /** Inserts the loan and sets its generated id. */
    public void insertLoan(Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (account_number, original_amount, years, monthly_payment, "
                + "start_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, loan.getAccountNumber());
            statement.setDouble(2, loan.getOriginalAmount());
            statement.setInt(3, loan.getYears());
            statement.setDouble(4, loan.getMonthlyPayment());
            statement.setDate(5, java.sql.Date.valueOf(loan.getStartDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                loan.setLoanId(keys.getInt(1));
            }
        }
    }

    /** @return the account's loans, in insertion order. */
    public List<Loan> loansFor(int accountNumber) throws SQLException {
        String sql = "SELECT loan_id, original_amount, years, monthly_payment, start_date "
                + "FROM loans WHERE account_number = ? ORDER BY loan_id";
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Loan loan = new Loan(accountNumber, rs.getDouble("original_amount"),
                            rs.getInt("years"), rs.getDouble("monthly_payment"),
                            rs.getDate("start_date").toLocalDate());
                    loan.setLoanId(rs.getInt("loan_id"));
                    loans.add(loan);
                }
            }
        }
        return loans;
    }

    // --------------------------------------------------------------- mortgages

    /** Inserts the mortgage and sets its generated id. */
    public void insertMortgage(Mortgage mortgage) throws SQLException {
        String sql = "INSERT INTO mortgages (account_number, original_amount, years, "
                + "monthly_payment, start_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, mortgage.getAccountNumber());
            statement.setDouble(2, mortgage.getOriginalAmount());
            statement.setInt(3, mortgage.getYears());
            statement.setDouble(4, mortgage.getMonthlyPayment());
            statement.setDate(5, java.sql.Date.valueOf(mortgage.getStartDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                mortgage.setMortgageId(keys.getInt(1));
            }
        }
    }

    /** @return the account's mortgages, in insertion order. */
    public List<Mortgage> mortgagesFor(int accountNumber) throws SQLException {
        String sql = "SELECT mortgage_id, original_amount, years, monthly_payment, start_date "
                + "FROM mortgages WHERE account_number = ? ORDER BY mortgage_id";
        List<Mortgage> mortgages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Mortgage mortgage = new Mortgage(accountNumber, rs.getDouble("original_amount"),
                            rs.getInt("years"), rs.getDouble("monthly_payment"),
                            rs.getDate("start_date").toLocalDate());
                    mortgage.setMortgageId(rs.getInt("mortgage_id"));
                    mortgages.add(mortgage);
                }
            }
        }
        return mortgages;
    }

    // ------------------------------------------------------------------- cards

    /** Inserts the card and sets its generated id. Debit cards store a NULL credit limit. */
    public void insertCard(Card card) throws SQLException {
        String sql = "INSERT INTO cards (account_number, card_type, credit_limit, issued_date) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, card.getAccountNumber());
            statement.setString(2, card.getCardType().name());
            if (card.getCardType() == Card.CardType.CREDIT) {
                statement.setDouble(3, card.getCreditLimit());
            } else {
                statement.setNull(3, java.sql.Types.DOUBLE);
            }
            statement.setDate(4, java.sql.Date.valueOf(card.getIssuedDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                card.setCardId(keys.getInt(1));
            }
        }
    }

    /** @return the account's cards, in insertion order. */
    public List<Card> cardsFor(int accountNumber) throws SQLException {
        String sql = "SELECT card_id, card_type, credit_limit, issued_date "
                + "FROM cards WHERE account_number = ? ORDER BY card_id";
        List<Card> cards = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Card.CardType type = Card.CardType.valueOf(rs.getString("card_type"));
                    double limit = (type == Card.CardType.CREDIT) ? rs.getDouble("credit_limit") : 0;
                    Card card = new Card(accountNumber, type, limit,
                            rs.getDate("issued_date").toLocalDate());
                    card.setCardId(rs.getInt("card_id"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    /** @return how many cards the account currently holds. */
    public int countCards(int accountNumber) throws SQLException {
        String sql = "SELECT COUNT(*) AS card_count FROM cards WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt("card_count");
            }
        }
    }
}
