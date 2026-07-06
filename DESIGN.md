# DESIGN — Bank Management System

The current architecture. For setup and usage see [README.md](README.md).

## 1. Overview

One unified system: an OOP business-logic layer backed by PostgreSQL
through plain JDBC. The program holds no in-memory account cache — every
operation reads and writes through the database, so state survives across
runs and is always consistent with what SQL queries see.

Stack: plain Java (JDK 11+), PostgreSQL JDBC driver, no ORM/framework/DI.

## 2. Layering

```
Main  →  menu  →  service  →  dao  →  db
```

One direction only; each layer knows nothing about the ones above it.

| Layer | Package / files | Responsibility | Never contains |
|---|---|---|---|
| Entry | `Main.java` | connect, seed if empty, main loop | business rules, SQL |
| Console | `menu/` | menus, input validation, output text | business rules, SQL |
| Service | `src/service/` | business rules, transaction boundaries | SQL, console IO |
| Data access | `src/dao/` | all SQL, as `PreparedStatement`s | business rules |
| Database | `src/db/` | JDBC connection, schema DDL | domain knowledge |

The domain model (`accounts/`, `bank/`, `products/`, `interfaces/`,
`exceptions/`) is shared by every layer.

- **`menu`** — one class per main-menu category (`AccountsMenu`,
  `MoneyMenu`, `ProductsMenu`, `ReportsMenu`) plus `ConsoleIO` (validated
  input, list printing, the attempt/reject wrapper) and `Strings` (every
  console literal as a constant — the program prints nothing not defined
  there). A rejected operation prints its reason and returns to the menu;
  only a database failure is fatal.
- **`service`** — `BankManager` owns everything state-changing: eligibility
  rules, the youth age gate, the exactly-one-owner rule, account numbering,
  and transaction boundaries (`inTransaction` wraps multi-table writes so
  they commit or roll back as one). `Reports` is the read-only side:
  listings, profit and fee queries, filtered purely on capability
  interfaces. Only `BankManager` can construct it (package-private
  constructor), handed out via `bank.reports()`.
- **`dao`** — `AccountDAO`, `ClientDAO`, `EmployeeDAO`, `ProductDAO`,
  `TransactionDAO`. Pure data access; they share `BankManager`'s
  `Connection` so its transaction boundaries span them. `AccountDAO` loads
  an account in **one** statement (JOIN clients + employees, LEFT JOIN all
  three details tables) — no N+1 queries.
- **`db`** — `DatabaseConnection` reads `db.properties` (never hardcoded
  credentials); `Schema` creates all tables idempotently
  (`CREATE TABLE IF NOT EXISTS`) on startup.

## 3. Domain model

```
Account (abstract, Comparable<Account>)
 ├─ CheckingAccount (abstract, Profitable; floor = -creditLimit)
 │   ├─ RegularCheckingAccount
 │   └─ BusinessCheckingAccount (ManagementFeeChargeable; VIP rule)
 ├─ SavingsAccount (plain; floor = 0)
 └─ YouthAccount   (plain; floor = 0; owner under 18 at opening)
```

- `deposit`/`withdraw` mutate the balance with a `minimumBalance()` floor
  hook (0 by default, `-creditLimit` for checking accounts);
  `restoreBalance` exists so reloading from the database doesn't reset
  balances to the default.
- `Client` (name, date of birth, rank 0–10) and `Employee` (id, name) are
  plain entities; ids are 0 until the DAO persists and back-fills them.
- `Profitable` / `ManagementFeeChargeable` are capability interfaces:
  reports only ever check these, never concrete classes, so a new account
  type never touches report logic.

**Products are tables, not account subclasses.** `Loan`, `Mortgage`
(id, account, original amount, years, monthly payment, start date) and
`Card` (debit — no limit / credit — positive limit) attach 0..N to an
eligible account. Eligibility in code is `instanceof CheckingAccount` —
exactly regular + business — so a future borrowing-capable type just
extends `CheckingAccount`. Youth and savings accounts are limited to one
card; youth debit-only.

## 4. Relational schema (10 tables, 3NF)

Created by `src/db/Schema.java`. Mapping: **Class Table Inheritance** — one
base `accounts` table plus one details table per concrete type that has
extra fields (youth has none, so no details table). Derived values (profit,
VIP) are never stored — computed in Java from loaded objects.

| Table | Key columns | Notes |
|---|---|---|
| `employees` | `employee_id` PK (user-chosen) | |
| `clients` | `client_id` SERIAL PK | `rank` CHECK 0–10, `date_of_birth` |
| `accounts` | `account_number` PK | type CHECK enum, `client_id` FK **RESTRICT**, `employee_id` FK **RESTRICT** |
| `checking_account_details` | PK/FK `account_number` **CASCADE** | credit_limit |
| `business_account_details` | PK/FK `account_number` **CASCADE** | credit_limit, business_revenue |
| `savings_account_details` | PK/FK `account_number` **CASCADE** | deposit_amount, years |
| `loans` | SERIAL PK, `account_number` FK **RESTRICT** | amount, years, payment, start |
| `mortgages` | SERIAL PK, `account_number` FK **RESTRICT** | same shape as loans |
| `cards` | SERIAL PK, `account_number` FK **CASCADE** | type CHECK, `credit_limit` NULL for debit |
| `transactions` | SERIAL PK, `account_number` FK **CASCADE** | type CHECK DEPOSIT/WITHDRAWAL, amount, occurred_at |

Cardinalities: client 1→N accounts (each account exactly one owner,
enforced in `BankManager.addAccount`); employee 1→N accounts; account 1→N
loans/mortgages/cards/transactions; account 1→0..1 details row.

Delete rules: deleting an account cascades its details, cards and
transactions but is **refused** while it has loans or mortgages; deleting a
client or employee is refused while they have accounts.

## 5. Transactions & seeding

- `BankManager.inTransaction(work)` sets autocommit off, runs the DAO
  calls, commits, and rolls back whole on any `SQLException`. Used by
  `addAccount` (base row + details + client/employee resolution) and by
  deposit/withdraw (balance update + movement log).
- `sample.java` seeds the demo data set (16 accounts, 3 employees, 2 loans,
  2 mortgages, 13 cards, 8 movements) **only when the database is empty**,
  entirely through the public service API — it contains zero SQL. Youth
  owners get relative dates of birth (now − 16/14/12 years) so the age gate
  never expires.

## 6. Known limitations

1. Large monetary values display in scientific notation (e.g. `1.2E7`) —
   calculation is unaffected; a formatting layer is planned.
2. Account numbers are `int` in Java — bounded, fine at course scale.
3. Client identity on seeding/resolution is case-insensitive name match.
