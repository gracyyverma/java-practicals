/*
 * ============================================================================
 *  LAB 09 - JDBC with MySQL: Full CRUD, Batch Processing & Transactions
 * ============================================================================
 *  Aim : Connect a Java application to MySQL. Create a student table and
 *        implement full CRUD using
 *        (1) Statement for simple queries
 *        (2) PreparedStatement for parameterized insert and update
 *        (3) batch processing to insert 10 records at once
 *        (4) transaction management - transfer marks between records with
 *            commit and rollback on failure.
 *        Display results using ResultSet.
 *
 *  CO5 : Develop database-connected Java applications using JDBC
 *  Run : java -cp "..\lib\*" Lab09.java          (Windows)
 *        java -cp "../lib/*" Lab09.java          (Linux / macOS)
 *  DB  : uses DB_URL / DB_USER / DB_PASS environment variables if set,
 *        otherwise localhost:3306, user root, password root.
 * ============================================================================
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lab09 {

    static final String URL  = env("DB_URL", "jdbc:mysql://localhost:3306/college_lab"
                                              + "?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true");
    static final String USER = env("DB_USER", "root");
    static final String PASS = env("DB_PASS", "root");

    public static void main(String[] args) {
        line('=');
        System.out.println("  LAB 09 : JDBC + MYSQL - CRUD, BATCH PROCESSING & TRANSACTIONS");
        line('=');

        // try-with-resources closes the Connection automatically
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            DatabaseMetaData md = con.getMetaData();
            System.out.println("  Connected to : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion()
                    + "  (database '" + con.getCatalog() + "')");
            System.out.println("  JDBC driver  : " + md.getDriverName() + " "
                    + md.getDriverVersion().replaceAll("\\s*\\(Revision.*", "") + "  (Type 4, pure Java)");

            // ==============================================================
            // (1) Statement - simple, fixed SQL (DDL + one INSERT + SELECT)
            // ==============================================================
            section("1. Statement - simple queries (CREATE, INSERT, SELECT)");
            try (Statement st = con.createStatement()) {
                st.executeUpdate("DROP TABLE IF EXISTS student");
                st.executeUpdate("CREATE TABLE student ("
                        + " id     INT PRIMARY KEY AUTO_INCREMENT,"
                        + " name   VARCHAR(40) NOT NULL,"
                        + " branch VARCHAR(10) NOT NULL,"
                        + " marks  INT NOT NULL,"
                        + " CONSTRAINT chk_marks CHECK (marks BETWEEN 0 AND 100))");
                System.out.println("  CREATE TABLE student (id, name, branch, marks)  + CHECK (marks BETWEEN 0 AND 100)");
                int rows = st.executeUpdate("INSERT INTO student (name, branch, marks) VALUES ('Gracy Sharma', 'CSE', 88)");
                System.out.println("  INSERT via Statement -> " + rows + " row inserted");
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS total FROM student")) {
                    rs.next();
                    System.out.println("  SELECT COUNT(*)      -> " + rs.getInt("total") + " student(s) in the table");
                }
            }

            // ==============================================================
            // (2) PreparedStatement - parameterized INSERT and UPDATE
            // ==============================================================
            section("2. PreparedStatement - parameterized INSERT and UPDATE");
            String insertSql = "INSERT INTO student (name, branch, marks) VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                Object[][] data = { {"Aman Gupta", "ECE", 72}, {"Riya Patel", "CSE", 91} };
                for (Object[] d : data) {
                    ps.setString(1, (String) d[0]);              // ? number 1
                    ps.setString(2, (String) d[1]);              // ? number 2
                    ps.setInt(3, (Integer) d[2]);                // ? number 3
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        System.out.printf("  INSERT %-11s -> generated id = %d%n", d[0], keys.getInt(1));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE student SET marks = ? WHERE name = ?")) {
                ps.setInt(1, 79);
                ps.setString(2, "Aman Gupta");
                System.out.println("  UPDATE marks = 79 WHERE name = 'Aman Gupta' -> " + ps.executeUpdate() + " row updated");
            }
            System.out.println("  (PreparedStatement is pre-compiled and safe from SQL injection)");

            // ==============================================================
            // (3) Batch processing - 10 records in one round-trip
            // ==============================================================
            section("3. Batch processing - insert 10 records at once");
            Object[][] batch = {
                {"Kabir Singh", "ME", 64}, {"Sana Khan", "ECE", 85}, {"Dev Malhotra", "ME", 58},
                {"Isha Verma", "CSE", 47}, {"Arjun Nair", "ECE", 69}, {"Meera Joshi", "CSE", 95},
                {"Rohan Das", "ME", 38}, {"Tara Bose", "ECE", 77}, {"Vikram Rao", "CSE", 82},
                {"Neha Iyer", "ME", 71} };
            con.setAutoCommit(false);                            // send the batch as one unit
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                for (Object[] d : batch) {
                    ps.setString(1, (String) d[0]);
                    ps.setString(2, (String) d[1]);
                    ps.setInt(3, (Integer) d[2]);
                    ps.addBatch();                               // queue it, do not run yet
                }
                int[] counts = ps.executeBatch();                // run all 10 together
                con.commit();
                System.out.println("  addBatch() x 10, executeBatch() -> " + Arrays.toString(counts));
                System.out.println("  committed " + Arrays.stream(counts).sum() + " rows in a single batch");
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
            System.out.println();
            System.out.println("  READ  ->  SELECT * FROM student   (displayed with ResultSet)");
            printQuery(con, "SELECT id, name, branch, marks FROM student ORDER BY id");

            // ==============================================================
            // (4) Transaction - transfer marks, COMMIT or ROLLBACK
            // ==============================================================
            section("4. Transaction - transfer marks with COMMIT and ROLLBACK");
            System.out.println("  Case A: move 4 marks from Meera (id 9) to Isha (id 7)");
            transferMarks(con, 9, 7, 4);
            printQuery(con, "SELECT id, name, marks FROM student WHERE id IN (7, 9) ORDER BY id");
            System.out.println("  Case B: move 10 marks from Isha (id 7) to Riya (id 3) -> Riya would get 101");
            transferMarks(con, 7, 3, 10);
            printQuery(con, "SELECT id, name, marks FROM student WHERE id IN (3, 7) ORDER BY id");

            // ==============================================================
            // DELETE - completes CRUD, then a summary through ResultSet
            // ==============================================================
            section("5. DELETE and a summary query");
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM student WHERE marks < ?")) {
                ps.setInt(1, 40);
                System.out.println("  DELETE FROM student WHERE marks < 40 -> " + ps.executeUpdate() + " row deleted");
            }
            printQuery(con, "SELECT branch, COUNT(*) AS students, ROUND(AVG(marks), 1) AS avg_marks, "
                    + "MAX(marks) AS top FROM student GROUP BY branch ORDER BY branch");

        } catch (SQLException e) {
            System.out.println("  DATABASE ERROR: " + e.getMessage());
            System.out.println("  Check that MySQL is running and DB_URL / DB_USER / DB_PASS are correct.");
            return;
        }

        line('=');
        System.out.println("  Lab 09 completed successfully - connection closed automatically");
        line('=');
    }

    /** Moves marks between two students inside ONE transaction. */
    static void transferMarks(Connection con, int fromId, int toId, int amount) throws SQLException {
        con.setAutoCommit(false);                                // BEGIN TRANSACTION
        try (PreparedStatement debit = con.prepareStatement("UPDATE student SET marks = marks - ? WHERE id = ?");
             PreparedStatement credit = con.prepareStatement("UPDATE student SET marks = marks + ? WHERE id = ?")) {
            debit.setInt(1, amount);
            debit.setInt(2, fromId);
            debit.executeUpdate();
            System.out.println("    step 1: -" + amount + " marks from id " + fromId + "  (done, not yet saved)");

            credit.setInt(1, amount);
            credit.setInt(2, toId);
            credit.executeUpdate();                              // may break the CHECK constraint
            System.out.println("    step 2: +" + amount + " marks to   id " + toId);

            con.commit();
            System.out.println("    COMMIT   -> both updates saved permanently");
        } catch (SQLException e) {
            con.rollback();
            System.out.println("    step 2 FAILED: " + e.getMessage());
            System.out.println("    ROLLBACK -> step 1 undone, no marks were lost");
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** Runs a SELECT and prints the ResultSet as a MySQL-style table. */
    static void printQuery(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            List<String[]> rows = new ArrayList<>();
            int[] width = new int[cols];
            String[] head = new String[cols];
            for (int c = 1; c <= cols; c++) {
                head[c - 1] = meta.getColumnLabel(c);
                width[c - 1] = head[c - 1].length();
            }
            while (rs.next()) {                                  // move the cursor row by row
                String[] r = new String[cols];
                for (int c = 1; c <= cols; c++) {
                    r[c - 1] = rs.getString(c);
                    width[c - 1] = Math.max(width[c - 1], r[c - 1].length());
                }
                rows.add(r);
            }
            StringBuilder sep = new StringBuilder("  +");
            for (int w : width) sep.append("-".repeat(w + 2)).append('+');
            System.out.println(sep);
            System.out.println(formatRow(head, width, meta, true));
            System.out.println(sep);
            for (String[] r : rows) System.out.println(formatRow(r, width, meta, false));
            System.out.println(sep);
            System.out.println("  " + rows.size() + " row(s) in set");
        }
    }

    static String formatRow(String[] v, int[] width, ResultSetMetaData meta, boolean header) throws SQLException {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < v.length; i++) {
            boolean numeric = !header && isNumeric(meta.getColumnType(i + 1));
            String cell = numeric ? String.format("%" + width[i] + "s", v[i]) : String.format("%-" + width[i] + "s", v[i]);
            sb.append(' ').append(cell).append(" |");
        }
        return sb.toString();
    }

    static boolean isNumeric(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.BIGINT || sqlType == Types.DECIMAL
            || sqlType == Types.NUMERIC || sqlType == Types.DOUBLE;
    }

    static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}
