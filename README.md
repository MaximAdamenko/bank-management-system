# Bank Management System

A console bank management system for a Database Systems course: an
object-oriented Java business layer backed by real PostgreSQL persistence
over plain JDBC. Accounts, clients, credit products and every deposit or
withdrawal survive across runs — the program is a thin client, the data
lives in the database.

No frameworks: plain Java, JDBC, and SQL. All SQL lives in DAO classes as
`PreparedStatement`s; multi-table writes run in real database transactions.

## Features

- **4 account types** — Regular Checking, Business Checking (with VIP
  status), Savings, and Youth (age-gated: owner must be under 18 at opening).
- **Credit products** — loans and mortgages, opened 0..N per eligible
  account; debit and credit cards with per-type limits.
- **Deposits & withdrawals** — balance floors enforced (overdraft up to the
  credit limit for checking accounts, 0 for the rest), every movement
  logged to a transactions table.
- **Reports** — annual profit per account / total, profitable accounts
  ranked, top checking contributor, management fees + CEO bonus.

| Account type | Loans / mortgages | Cards |
|---|---|---|
| Regular Checking | yes | unlimited, debit + credit |
| Business Checking | yes | unlimited, debit + credit |
| Savings | no | at most 1 (either type) |
| Youth | no | at most 1, debit only |

## Requirements

- **JDK 11+** (`javac`/`java` on PATH)
- **PostgreSQL** server running on `localhost:5432`
- **PostgreSQL JDBC driver** — download
  [postgresql-42.7.7.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar)
  and place it in the project root (it is deliberately not committed).

## Setup

1. Create the database (schema is created automatically on first run):

   ```bash
   createdb -h localhost -U postgres bankdb
   ```

2. Configure credentials — copy the template and fill in your password:

   ```bash
   cp src/db/db.properties.example src/db/db.properties
   # edit src/db/db.properties: db.password=<your postgres password>
   ```

   `db.properties` is git-ignored, so real credentials never reach the repo.

## Compile & run

From the project root:

```bash
javac -cp .:src $(find . -name "*.java")
java -cp .:src:postgresql-42.7.7.jar Main
```

(On Windows use `;` instead of `:` in the classpath.)

First launch against an empty database prints `Empty database - loading
sample data...` and seeds 16 accounts, 3 employees, 2 loans, 2 mortgages,
13 cards and 8 movements. Later runs skip seeding and show the same data —
including anything you changed.

## Using the menu

Navigate by typing a number + Enter; `0` goes back (or exits, from the main
menu). Six categories: Accounts, Deposits & withdrawals, Loans, Mortgages,
Cards, Reports. Invalid input and rejected operations (e.g. overdrawing a
savings account, issuing a credit card to a youth account) print their
reason and return to the menu — only a database failure ends the program.

## Project layout

```
Main.java          entry point: connect, seed if empty, main menu loop
sample.java        idempotent sample-data seeder (uses the service API, no SQL)
menu/              console layer: one class per menu + ConsoleIO + Strings
src/service/       business rules and transaction boundaries (no SQL)
src/dao/           all SQL, as PreparedStatements (no business rules)
src/db/            JDBC connection (db.properties) + schema DDL
src/accounts/      Account class hierarchy
src/bank/          Client, Employee
src/products/      Loan, Mortgage, Card, Transaction
src/interfaces/    Profitable, ManagementFeeChargeable
src/exceptions/    DuplicationException
```

See [DESIGN.md](DESIGN.md) for the architecture and the 10-table schema.
