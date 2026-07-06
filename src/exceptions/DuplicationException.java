package exceptions;

/**
 * Signals that an account number is already taken.
 *
 * Checked, because callers (the menu) are expected to catch it and ask the
 * user for a different number rather than let the program crash.
 */
public class DuplicationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int duplicateAccountNumber;

    public DuplicationException(int duplicateAccountNumber) {
        super("Account number " + duplicateAccountNumber + " is already in use. Please choose a different one.");
        this.duplicateAccountNumber = duplicateAccountNumber;
    }

    public int getDuplicateAccountNumber() {
        return duplicateAccountNumber;
    }
}
