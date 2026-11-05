/*
 * ============================================================================
 *  LAB 01 - Java Basics: Loops, Recursion, Methods
 * ============================================================================
 *  Aim : Write Java programs to demonstrate
 *        (1) printing patterns using nested loops
 *        (2) finding factorial using recursion
 *        (3) checking prime numbers in a range
 *        (4) reversing a string without using built-in methods
 *
 *  CO1 : Understand Java environment and basic programming constructs
 *  Run : java Lab01.java          (Java 11+ runs a single source file directly)
 * ============================================================================
 */
public class Lab01 {

    public static void main(String[] args) {
        banner("LAB 01 : JAVA BASICS - LOOPS, RECURSION & METHODS");

        // ---------------------------------------------------------------
        // (1) Patterns using nested loops
        // ---------------------------------------------------------------
        section("1. Patterns using nested loops (n = 5)");
        int n = 5;
        System.out.println("  a) Right triangle      b) Pyramid             c) Floyd's triangle");
        String[] a = rightTriangle(n);
        String[] b = pyramid(n);
        String[] c = floydsTriangle(n);
        for (int i = 0; i < n; i++) {
            System.out.printf("     %-18s %-22s %s%n", a[i], b[i], c[i]);
        }

        // ---------------------------------------------------------------
        // (2) Factorial using recursion
        // ---------------------------------------------------------------
        section("2. Factorial using recursion");
        System.out.println("  Recursion trace for factorial(5):");
        long result = factorialTrace(5, 1);
        System.out.println("  => 5! = " + result);
        System.out.print("  Table : ");
        int[] values = {0, 1, 7, 10, 15, 20};
        for (int v : values) {
            System.out.print(v + "! = " + factorial(v) + (v == 20 ? "" : " | "));
        }
        System.out.println();

        // ---------------------------------------------------------------
        // (3) Prime numbers in a range
        // ---------------------------------------------------------------
        section("3. Prime numbers in a range [1, 100]");
        int count = 0;
        StringBuilder line = new StringBuilder("  ");
        for (int i = 1; i <= 100; i++) {
            if (isPrime(i)) {
                line.append(String.format("%3d ", i));
                count++;
                if (count % 13 == 0) {           // wrap every 13 primes
                    System.out.println(line);
                    line = new StringBuilder("  ");
                }
            }
        }
        if (line.length() > 2) System.out.println(line);
        System.out.println("  Total primes between 1 and 100 = " + count);
        System.out.println("  isPrime(97) = " + isPrime(97) + ", isPrime(91) = " + isPrime(91) + "  (91 = 7 x 13)");

        // ---------------------------------------------------------------
        // (4) Reverse a string without built-in reverse()
        // ---------------------------------------------------------------
        section("4. Reversing a string without built-in methods");
        String[] words = {"Gracy", "Java Lab", "racecar", "OpenJDK 17"};
        for (String w : words) {
            String r = reverse(w);
            System.out.printf("  %-12s -> %-12s %s%n", "\"" + w + "\"", "\"" + r + "\"",
                    sameText(w, r) ? "(palindrome!)" : "");
        }

        footer("Lab 01 completed successfully");
    }

    // ===================== (1) PATTERN METHODS ===========================

    /** Right-angled triangle of stars: row i has i stars. */
    static String[] rightTriangle(int n) {
        String[] rows = new String[n];
        for (int i = 1; i <= n; i++) {            // outer loop -> rows
            String row = "";
            for (int j = 1; j <= i; j++) {        // inner loop -> columns
                row += "* ";
            }
            rows[i - 1] = row;
        }
        return rows;
    }

    /** Centered pyramid: (n - i) spaces followed by (2i - 1) stars. */
    static String[] pyramid(int n) {
        String[] rows = new String[n];
        for (int i = 1; i <= n; i++) {
            String row = "";
            for (int s = 1; s <= n - i; s++) row += " ";      // leading spaces
            for (int k = 1; k <= 2 * i - 1; k++) row += "*";  // stars
            rows[i - 1] = row;
        }
        return rows;
    }

    /** Floyd's triangle: consecutive natural numbers row by row. */
    static String[] floydsTriangle(int n) {
        String[] rows = new String[n];
        int num = 1;
        for (int i = 1; i <= n; i++) {
            String row = "";
            for (int j = 1; j <= i; j++) {
                row += String.format("%-3d", num++);
            }
            rows[i - 1] = row;
        }
        return rows;
    }

    // ===================== (2) RECURSION ================================

    /** Classic recursive factorial: n! = n * (n-1)!, base case 0! = 1! = 1 */
    static long factorial(int n) {
        if (n <= 1) return 1;                 // base case stops the recursion
        return n * factorial(n - 1);          // recursive case
    }

    /** Same recursion, but prints each call so we can SEE the call stack. */
    static long factorialTrace(int n, int depth) {
        String indent = "    ".repeat(depth);
        if (n <= 1) {
            System.out.println(indent + "factorial(" + n + ") = 1   <- base case");
            return 1;
        }
        System.out.println(indent + "factorial(" + n + ") = " + n + " * factorial(" + (n - 1) + ")");
        long value = n * factorialTrace(n - 1, depth + 1);
        System.out.println(indent + "factorial(" + n + ") returns " + value);
        return value;
    }

    // ===================== (3) PRIME CHECK ==============================

    /** A number is prime if it has no divisor between 2 and sqrt(n). */
    static boolean isPrime(int num) {
        if (num < 2) return false;
        for (int d = 2; d * d <= num; d++) {
            if (num % d == 0) return false;   // found a divisor -> not prime
        }
        return true;
    }

    // ===================== (4) STRING REVERSAL ==========================

    /**
     * Reverses a string using the two-pointer swap technique.
     * No StringBuilder.reverse(), no Collections.reverse() - just a loop.
     */
    static String reverse(String str) {
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        while (left < right) {
            char temp = chars[left];          // swap the two ends
            chars[left] = chars[right];
            chars[right] = temp;
            left++;
            right--;
        }
        return new String(chars);
    }

    /** Manual character-by-character comparison (no equals()). */
    static boolean sameText(String x, String y) {
        char[] p = x.toCharArray(), q = y.toCharArray();
        if (p.length != q.length) return false;
        for (int i = 0; i < p.length; i++) if (p[i] != q[i]) return false;
        return true;
    }

    // ===================== OUTPUT HELPERS ===============================

    static void banner(String title) {
        String line = "=".repeat(74);
        System.out.println(line);
        System.out.println("  " + title);
        System.out.println(line);
    }

    static void section(String title) {
        System.out.println();
        System.out.println(">> " + title);
        System.out.println("-".repeat(74));
    }

    static void footer(String msg) {
        System.out.println();
        System.out.println("=".repeat(74));
        System.out.println("  " + msg);
        System.out.println("=".repeat(74));
    }
}
