/*
 * ============================================================================
 *  LAB 06 - String Operations & File Input/Output
 * ============================================================================
 *  Aim : Write programs to
 *        (1) perform String operations - palindrome check, word count,
 *            character frequency
 *        (2) compare String, StringBuffer and StringBuilder performance
 *        (3) write student records to a text file using BufferedWriter and
 *            read them back using BufferedReader
 *        (4) serialize a Student object to a file and deserialize it back
 *
 *  CO3 : Perform string operations and manage file input/output in Java
 *  Run : java Lab06.java       (creates students.txt and student.ser here)
 * ============================================================================
 */
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Lab06 {

    public static void main(String[] args) throws Exception {
        line('=');
        System.out.println("  LAB 06 : STRINGS, STRINGBUFFER/BUILDER & FILE I/O");
        line('=');

        // ===================================================================
        // (1) STRING OPERATIONS
        // ===================================================================
        section("1a. Palindrome check (ignores case, spaces and punctuation)");
        String[] samples = { "Madam", "A man, a plan, a canal: Panama", "Gracy", "Was it a car or a cat I saw?" };
        for (String s : samples) {
            System.out.printf("  %-34s -> %s%n", "\"" + s + "\"", isPalindrome(s) ? "PALINDROME" : "not a palindrome");
        }

        section("1b. Word count & 1c. Character frequency");
        String sentence = "Java is simple and Java is powerful";
        String[] words = sentence.trim().split("\\s+");
        System.out.println("  Sentence    : \"" + sentence + "\"");
        System.out.println("  Total words : " + words.length);
        Map<String, Integer> wordFreq = new LinkedHashMap<>();
        for (String w : words) wordFreq.merge(w.toLowerCase(), 1, Integer::sum);
        System.out.println("  Word freq   : " + wordFreq);

        String text = "programming";
        Map<Character, Integer> charFreq = new LinkedHashMap<>();
        for (char ch : text.toCharArray()) charFreq.merge(ch, 1, Integer::sum);
        System.out.println("  Char freq of \"" + text + "\" : " + charFreq);
        char top = ' '; int best = 0;
        for (Map.Entry<Character, Integer> e : charFreq.entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
        }
        System.out.println("  Most frequent character : '" + top + "' (" + best + " times)");

        // ===================================================================
        // (2) String vs StringBuffer vs StringBuilder
        // ===================================================================
        section("2. Performance: String vs StringBuffer vs StringBuilder");
        String s1 = "Java";
        String s2 = s1.concat(" Lab");
        System.out.println("  Immutability: s1 = \"" + s1 + "\" after s1.concat(\" Lab\") -> new object s2 = \"" + s2 + "\"");
        int n = 30_000;
        timeAll(2_000);                                   // warm-up run so the JIT is fair to everyone
        long[] t = timeAll(n);
        System.out.println("  Appending one character " + String.format("%,d", n) + " times:");
        System.out.printf("  %-14s| %-8s| %-12s| %12s | %s%n", "Class", "Mutable", "Thread-safe", "Time (ms)", "vs StringBuilder");
        System.out.println("  " + "-".repeat(14) + "+" + "-".repeat(9) + "+" + "-".repeat(13) + "+" + "-".repeat(14) + "+" + "-".repeat(17));
        String[] names = { "String", "StringBuffer", "StringBuilder" };
        String[] mutable = { "No", "Yes", "Yes" };
        String[] safe = { "Yes", "Yes (sync)", "No" };
        for (int i = 0; i < 3; i++) {
            System.out.printf("  %-14s| %-8s| %-12s| %12.3f | %8.1fx slower%n",
                    names[i], mutable[i], safe[i], t[i] / 1e6, (double) t[i] / t[2]);
        }
        System.out.println("  => String creates a NEW object on every +=, so it is by far the slowest.");

        // ===================================================================
        // (3) BufferedWriter / BufferedReader
        // ===================================================================
        section("3. Write records with BufferedWriter, read back with BufferedReader");
        File txt = new File("students.txt");
        String[] records = { "101,Gracy Sharma,CSE,88.5", "102,Aman Gupta,ECE,76.0",
                             "103,Riya Patel,CSE,91.2", "104,Kabir Singh,ME,64.8" };
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(txt))) {   // auto-closed
            bw.write("roll,name,branch,percentage");
            bw.newLine();
            for (String r : records) { bw.write(r); bw.newLine(); }
        }
        System.out.println("  Wrote " + records.length + " records to " + txt.getName() + " (" + txt.length() + " bytes)");
        System.out.println();
        System.out.printf("  %-5s | %-13s | %-6s | %s%n", "Roll", "Name", "Branch", "Percent");
        System.out.println("  " + "-".repeat(6) + "+" + "-".repeat(15) + "+" + "-".repeat(8) + "+" + "-".repeat(8));
        double total = 0; int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(txt))) {
            br.readLine();                                // skip header
            String lineRead;
            while ((lineRead = br.readLine()) != null) {
                String[] p = lineRead.split(",");
                System.out.printf("  %-5s | %-13s | %-6s | %6.1f%n", p[0], p[1], p[2], Double.parseDouble(p[3]));
                total += Double.parseDouble(p[3]);
                count++;
            }
        }
        System.out.printf("  Read %d records back, class average = %.2f%%%n", count, total / count);

        // ===================================================================
        // (4) Serialization / Deserialization
        // ===================================================================
        section("4. Serialize a Student object, then deserialize it");
        Student original = new Student(101, "Gracy Sharma", 9.1, "gracy@123");
        System.out.println("  Before : " + original);
        File ser = new File("student.ser");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ser))) {
            out.writeObject(original);                    // object -> bytes
        }
        System.out.println("  Serialized to " + ser.getName() + " (" + ser.length() + " bytes)");
        Student restored;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ser))) {
            restored = (Student) in.readObject();         // bytes -> object
        }
        System.out.println("  After  : " + restored);
        System.out.println("  Same object in memory? " + (original == restored)
                + "  |  password is transient -> not saved, so it comes back as null");

        line('=');
        System.out.println("  Lab 06 completed successfully");
        line('=');
    }

    /** Two-pointer palindrome check on letters/digits only. */
    static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            char a = s.charAt(i), b = s.charAt(j);
            if (!Character.isLetterOrDigit(a)) { i++; continue; }
            if (!Character.isLetterOrDigit(b)) { j--; continue; }
            if (Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
            i++; j--;
        }
        return true;
    }

    /** Returns nanoseconds taken by String, StringBuffer and StringBuilder. */
    static long[] timeAll(int n) {
        long start = System.nanoTime();
        String str = "";
        for (int i = 0; i < n; i++) str += "x";            // new String object each time
        long tString = System.nanoTime() - start;

        start = System.nanoTime();
        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < n; i++) sbf.append("x");       // synchronized append
        long tBuffer = System.nanoTime() - start;

        start = System.nanoTime();
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < n; i++) sbd.append("x");       // unsynchronized append
        long tBuilder = System.nanoTime() - start;

        if (str.length() != sbf.length() || sbf.length() != sbd.length()) throw new AssertionError();
        return new long[] { tString, tBuffer, tBuilder };
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

/** A Serializable class. 'transient' fields are skipped during serialization. */
class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int roll;
    private final String name;
    private final double cgpa;
    private final transient String password;     // sensitive -> never written to disk

    Student(int roll, String name, double cgpa, String password) {
        this.roll = roll;
        this.name = name;
        this.cgpa = cgpa;
        this.password = password;
    }

    @Override
    public String toString() {
        return "Student{roll=" + roll + ", name='" + name + "', cgpa=" + cgpa + ", password=" + password + "}";
    }
}
