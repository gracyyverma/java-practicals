/*
 * ============================================================================
 *  LAB 05 - Exception Handling (Student Grade System)
 * ============================================================================
 *  Aim : Write a program simulating a student grade system. Create a custom
 *        exception InvalidMarksException thrown when marks are out of range
 *        (0-100). Demonstrate try-catch-finally, multi-catch blocks, throw
 *        and throws. Handle both checked (IOException) and unchecked
 *        (ArithmeticException, ArrayIndexOutOfBoundsException) exceptions in
 *        separate scenarios.
 *
 *  CO3 : Implement robust exception handling in Java applications
 *  Run : java Lab05.java
 * ============================================================================
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Lab05 {

    public static void main(String[] args) {
        line('=');
        System.out.println("  LAB 05 : STUDENT GRADE SYSTEM - EXCEPTION HANDLING");
        line('=');

        // ------------------------------------------------------------------
        // SCENARIO 1 : custom CHECKED exception + throw + throws + finally
        // ------------------------------------------------------------------
        section("Scenario 1 : Custom exception InvalidMarksException (throw / throws)");
        int[][] attempts = { {88, 92, 79}, {67, 105, 81}, {-5, 70, 90} };
        String[] names = { "Gracy", "Aman", "Riya" };
        for (int i = 0; i < attempts.length; i++) {
            try {
                String grade = GradeSystem.processStudent(names[i], attempts[i]);   // may throw
                System.out.println("  [TRY]     " + names[i] + " -> grade " + grade);
            } catch (InvalidMarksException e) {
                System.out.println("  [CATCH]   " + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                System.out.println("  [FINALLY] record of " + names[i] + " closed (finally ALWAYS runs)");
            }
        }

        // ------------------------------------------------------------------
        // SCENARIO 2 : CHECKED exception - IOException
        // ------------------------------------------------------------------
        section("Scenario 2 : Checked exception - IOException (file not found)");
        try {
            String data = GradeSystem.readMarksFile("marks_semester3.txt");    // declared 'throws IOException'
            System.out.println("  File content: " + data);
        } catch (IOException e) {
            System.out.println("  [CATCH]   " + e.getClass().getSimpleName() + " (a subclass of IOException)");
            System.out.println("            message : " + e.getMessage());
            System.out.println("            The compiler FORCED us to handle this: it is a checked exception.");
        } finally {
            System.out.println("  [FINALLY] file-reading attempt finished");
        }

        // ------------------------------------------------------------------
        // SCENARIO 3 : UNCHECKED - ArithmeticException
        // ------------------------------------------------------------------
        section("Scenario 3 : Unchecked exception - ArithmeticException");
        int[] noSubjects = {};
        try {
            int avg = GradeSystem.average(noSubjects);        // integer division by 0
            System.out.println("  Average = " + avg);
        } catch (ArithmeticException e) {
            System.out.println("  [CATCH]   ArithmeticException: " + e.getMessage());
            System.out.println("            Student has 0 subjects -> total / 0 is undefined for int.");
        }

        // ------------------------------------------------------------------
        // SCENARIO 4 : UNCHECKED - ArrayIndexOutOfBoundsException
        // ------------------------------------------------------------------
        section("Scenario 4 : Unchecked exception - ArrayIndexOutOfBoundsException");
        int[] marks = {75, 82, 91};
        try {
            System.out.println("  Subject 1 marks = " + marks[0]);
            System.out.println("  Subject 5 marks = " + marks[4]);       // only indexes 0..2 exist
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("  [CATCH]   ArrayIndexOutOfBoundsException: " + e.getMessage());
        }

        // ------------------------------------------------------------------
        // SCENARIO 5 : MULTI-CATCH  catch (A | B e)
        // ------------------------------------------------------------------
        section("Scenario 5 : Multi-catch  catch (ArithmeticException | ArrayIndexOutOfBoundsException e)");
        int[][] reportCards = { {80, 90}, {}, {70, 60} };
        int[] askForSubject = { 1, 0, 3 };
        for (int i = 0; i < reportCards.length; i++) {
            try {
                int avg = GradeSystem.average(reportCards[i]);
                int pick = reportCards[i][askForSubject[i]];
                System.out.println("  Card " + (i + 1) + " : average = " + avg + ", subject " + askForSubject[i] + " = " + pick);
            } catch (ArithmeticException | ArrayIndexOutOfBoundsException e) {
                System.out.println("  Card " + (i + 1) + " : one handler caught -> " + e.getClass().getSimpleName());
            }
        }

        // ------------------------------------------------------------------
        // SCENARIO 6 : exception propagation through 'throws'
        // ------------------------------------------------------------------
        section("Scenario 6 : Propagation up the call stack (throws)");
        try {
            GradeSystem.generateReport();                      // report -> calculate -> validate
        } catch (InvalidMarksException e) {
            System.out.println("  Caught in main(): " + e.getMessage());
            System.out.println("  Bad value stored in the exception object: " + e.getMarks());
            System.out.print("  Path it travelled: ");
            StackTraceElement[] st = e.getStackTrace();      // [0] = where it was thrown
            for (int i = 0; i <= 3; i++) System.out.print(st[i].getMethodName() + (i < 3 ? " -> " : "\n"));
        }

        System.out.println();
        System.out.printf("  %-20s| %-28s| %s%n", "Type", "Examples here", "Must handle/declare?");
        System.out.println("  " + "-".repeat(20) + "+" + "-".repeat(29) + "+" + "-".repeat(21));
        System.out.printf("  %-20s| %-28s| %s%n", "Checked", "IOException, InvalidMarks..", "YES (compile error)");
        System.out.printf("  %-20s| %-28s| %s%n", "Unchecked (Runtime)", "Arithmetic, ArrayIndex...", "NO (optional)");

        line('=');
        System.out.println("  Lab 05 completed successfully");
        line('=');
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

// ============================================================================
//  CUSTOM EXCEPTION - extends Exception, so it is a CHECKED exception
// ============================================================================
class InvalidMarksException extends Exception {
    private final int marks;

    InvalidMarksException(String studentName, int marks) {
        super("Invalid marks " + marks + " for " + studentName + " (allowed range is 0-100)");
        this.marks = marks;
    }

    int getMarks() { return marks; }
}

// ============================================================================
//  BUSINESS LOGIC
// ============================================================================
class GradeSystem {

    /** 'throws' declares that this method may pass the exception to its caller. */
    static void validateMarks(String name, int marks) throws InvalidMarksException {
        if (marks < 0 || marks > 100) {
            throw new InvalidMarksException(name, marks);     // 'throw' actually raises it
        }
    }

    static String processStudent(String name, int[] marks) throws InvalidMarksException {
        for (int m : marks) validateMarks(name, m);
        int avg = average(marks);
        return gradeFor(avg) + " (average " + avg + ")";
    }

    static int average(int[] marks) {
        int sum = 0;
        for (int m : marks) sum += m;
        return sum / marks.length;                            // ArithmeticException if length == 0
    }

    static String gradeFor(int avg) {
        if (avg >= 90) return "A+";
        if (avg >= 80) return "A";
        if (avg >= 70) return "B";
        if (avg >= 60) return "C";
        if (avg >= 40) return "D";
        return "F";
    }

    /** Checked exception: the caller MUST catch IOException or declare it. */
    static String readMarksFile(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {   // try-with-resources
            return br.readLine();
        }
    }

    // three-level call chain used in Scenario 6
    static void generateReport() throws InvalidMarksException { calculateResult(); }
    static void calculateResult() throws InvalidMarksException { validateMarks("Kabir", 150); }
}
