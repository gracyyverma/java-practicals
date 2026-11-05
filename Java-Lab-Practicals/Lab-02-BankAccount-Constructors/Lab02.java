/*
 * ============================================================================
 *  LAB 02 - Classes, Objects & Constructors (BankAccount)
 * ============================================================================
 *  Aim : Create a class BankAccount with attributes (account number, holder
 *        name, balance). Implement default, parameterized and copy
 *        constructors. Add methods for deposit, withdrawal (with balance
 *        check) and display. Create multiple objects and demonstrate
 *        constructor overloading.
 *
 *  CO1 : Implement OOP fundamentals using classes, objects and constructors
 *  Run : java Lab02.java
 * ============================================================================
 */
public class Lab02 {

    public static void main(String[] args) {
        line('=');
        System.out.println("  LAB 02 : BANK ACCOUNT - CLASSES, OBJECTS & CONSTRUCTORS");
        line('=');

        // ---- 1. Creating objects with different (overloaded) constructors ----
        section("1. Creating objects using overloaded constructors");
        // Note: each delegating constructor calls this(...) FIRST, so the
        // 3-arg constructor's message always prints before the caller's message.
        System.out.println("  new BankAccount()");
        BankAccount acc1 = new BankAccount();                                  // default
        System.out.println("  new BankAccount(\"Gracy Sharma\", 5000)");
        BankAccount acc2 = new BankAccount("Gracy Sharma", 5000.00);           // 2-arg
        System.out.println("  new BankAccount(\"SB-1003\", \"Rahul Verma\", 12500.50)");
        BankAccount acc3 = new BankAccount("SB-1003", "Rahul Verma", 12500.50); // 3-arg
        System.out.println("  new BankAccount(acc3)");
        BankAccount acc4 = new BankAccount(acc3);                              // copy

        // ---- 2. Display all accounts ----
        section("2. Account details");
        BankAccount[] all = {acc1, acc2, acc3, acc4};
        BankAccount.printHeader();
        for (BankAccount a : all) a.display();

        // ---- 3. Deposit & withdrawal with balance check ----
        section("3. Deposit and withdrawal (with balance check)");
        acc2.deposit(2500);
        acc2.withdraw(1000);
        acc2.withdraw(10000);          // more than balance -> rejected
        acc1.deposit(-50);             // invalid amount   -> rejected
        acc1.deposit(750);

        // ---- 4. Copy constructor creates an INDEPENDENT object ----
        section("4. Copy constructor creates an independent copy");
        System.out.println("  Withdrawing 2,500 from the COPY (acc4) only...");
        acc4.withdraw(2500);
        System.out.printf("  acc3 (original) balance : Rs. %,10.2f%n", acc3.getBalance());
        System.out.printf("  acc4 (copy)     balance : Rs. %,10.2f%n", acc4.getBalance());
        System.out.println("  Same object?  acc3 == acc4  ->  " + (acc3 == acc4));

        // ---- 5. Final state ----
        section("5. Final state of all accounts");
        BankAccount.printHeader();
        for (BankAccount a : all) a.display();
        System.out.println("\n  Total objects created : " + BankAccount.getObjectCount());

        line('=');
        System.out.println("  Lab 02 completed successfully");
        line('=');
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(74)); }
}

/**
 * BankAccount demonstrates constructor overloading: four constructors with
 * different parameter lists, chained together with this(...).
 */
class BankAccount {
    private String accountNumber;
    private String holderName;
    private double balance;

    private static int objectCount = 0;      // shared by all objects
    private static int nextNumber = 1001;    // used to auto-generate account numbers

    // (a) DEFAULT constructor - no arguments
    BankAccount() {
        this("SB-" + nextNumber++, "Not Assigned", 0.0);   // delegates to 3-arg constructor
        System.out.println("  [Default constructor]       -> " + accountNumber + " created");
    }

    // (b) PARAMETERIZED constructor - 2 arguments (auto account number)
    BankAccount(String holderName, double openingBalance) {
        this("SB-" + nextNumber++, holderName, openingBalance);
        System.out.println("  [2-arg constructor]         -> " + accountNumber + " created for " + holderName);
    }

    // (c) PARAMETERIZED constructor - 3 arguments (the "main" constructor)
    BankAccount(String accountNumber, String holderName, double balance) {
        this.accountNumber = accountNumber;   // 'this' resolves field vs parameter
        this.holderName = holderName;
        this.balance = balance;
        objectCount++;
        System.out.printf("  [3-arg constructor]         -> fields set (%s, %s, %.2f)%n",
                accountNumber, holderName, balance);
    }

    // (d) COPY constructor - builds a new object from an existing one
    BankAccount(BankAccount other) {
        this(other.accountNumber + "-COPY", other.holderName, other.balance);
        System.out.println("  [Copy constructor]          -> " + accountNumber + " copied from " + other.accountNumber);
    }

    /** Adds money; rejects zero or negative amounts. */
    void deposit(double amount) {
        if (amount <= 0) {
            System.out.printf("  %-12s DEPOSIT  Rs. %,9.2f  -> REJECTED (amount must be positive)%n", accountNumber, amount);
            return;
        }
        balance += amount;
        System.out.printf("  %-12s DEPOSIT  Rs. %,9.2f  -> OK, new balance Rs. %,10.2f%n", accountNumber, amount, balance);
    }

    /** Withdraws money only if the balance is sufficient. */
    boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            System.out.printf("  %-12s WITHDRAW Rs. %,9.2f  -> REJECTED (insufficient balance: Rs. %,.2f)%n",
                    accountNumber, amount, balance);
            return false;
        }
        balance -= amount;
        System.out.printf("  %-12s WITHDRAW Rs. %,9.2f  -> OK, new balance Rs. %,10.2f%n", accountNumber, amount, balance);
        return true;
    }

    static void printHeader() {
        System.out.printf("  %-14s | %-16s | %14s%n", "Account No.", "Holder Name", "Balance (Rs.)");
        System.out.println("  " + "-".repeat(15) + "+" + "-".repeat(18) + "+" + "-".repeat(16));
    }

    void display() {
        System.out.printf("  %-14s | %-16s | %,14.2f%n", accountNumber, holderName, balance);
    }

    double getBalance() { return balance; }
    static int getObjectCount() { return objectCount; }
}
