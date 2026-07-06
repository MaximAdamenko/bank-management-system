package bank;

/**
 * The bank employee who approves an account.
 *
 * Kept as its own class rather than a plain name String so that an employee
 * can gain more attributes later without touching any account code.
 */
public class Employee {

    private final int id;
    private final String name;

    public Employee(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be a positive number.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name must not be empty.");
        }
        this.id = id;
        this.name = name.trim();
    }

    /** Deep-copy constructor. */
    public Employee(Employee other) {
        this.id = other.id;
        this.name = other.name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (id=" + id + ")";
    }
}
