/*
 * ============================================================================
 *  LAB 10 - Advanced JDBC + Java 8+ Collectors + Records & Sealed Classes
 * ============================================================================
 *  Aim : Write programs to demonstrate
 *   (1) JDBC - connect to MySQL using all 4 driver types (conceptual
 *       comparison); implement CRUD using Statement, PreparedStatement and
 *       CallableStatement; handle ResultSet to display records; perform batch
 *       insert of 10 student records; manage transactions with commit,
 *       rollback and savepoint; configure a HikariCP connection pool.
 *   (2) Use Collectors.groupingBy() to group students by grade,
 *       partitioningBy() to split pass/fail, joining() to concatenate names;
 *       demonstrate flatMap on a list of course lists and reduce to compute
 *       total marks; chain Optional methods to safely retrieve a student's
 *       department.
 *   (3) Create a Record class for Student (id, name, marks) and demonstrate
 *       immutability; define a Sealed class hierarchy Shape with permitted
 *       subclasses Circle, Rectangle, Triangle.
 *
 *  CO5 : Develop database-connected Java applications using JDBC and apply
 *        advanced Java 8+ and modern Java features (Records, Sealed classes)
 *  Run : java -cp "..\lib\*" Lab10.java          (Windows, needs Java 17+)
 *        java -cp "../lib/*" Lab10.java          (Linux / macOS)
 *  DB  : uses DB_URL / DB_USER / DB_PASS environment variables if set,
 *        otherwise localhost:3306, user root, password root.
 * ============================================================================
 */
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.tools.*;

public class Lab10 {

    static final String URL  = env("DB_URL", "jdbc:mysql://localhost:3306/college_lab"
                                              + "?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true");
    static final String USER = env("DB_USER", "root");
    static final String PASS = env("DB_PASS", "root");

    /** student id -> department code, filled from the database in Part 1 */
    static final Map<Integer, String> DEPT_CODE = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // shorter log lines from HikariCP's logger (slf4j-simple)
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");

        line('=');
        System.out.println("  LAB 10 : ADVANCED JDBC  +  COLLECTORS & OPTIONAL  +  RECORDS & SEALED CLASSES");
        line('=');

        List<Student> students = partOneJdbc();
        partTwoStreams(students);
        partThreeRecordsAndSealed();

        System.out.println();
        line('=');
        System.out.println("  Lab 10 completed successfully");
        line('=');
    }

    // ########################################################################
    //  PART 1 - JDBC
    // ########################################################################
    static List<Student> partOneJdbc() throws SQLException {
        part("PART 1 - JDBC");

        // ---- 1.1 Driver types ----
        section("1.1 The 4 JDBC driver types (conceptual comparison + live check)");
        System.out.printf("  %-4s | %-17s | %-32s | %-4s | %s%n", "Type", "Name", "Path to the database", "Java", "Example / status");
        System.out.println("  " + "-".repeat(5) + "+" + "-".repeat(19) + "+" + "-".repeat(34) + "+" + "-".repeat(6) + "+" + "-".repeat(26));
        driverRow("1", "JDBC-ODBC Bridge", "JDBC -> ODBC driver -> DB", "No", "REMOVED in Java 8");
        driverRow("2", "Native-API", "JDBC -> vendor C library -> DB", "Part", "Oracle OCI driver");
        driverRow("3", "Network Protocol", "JDBC -> middleware server -> DB", "Yes", "IDS Server, DataDirect");
        driverRow("4", "Thin / Pure Java", "JDBC -> DB wire protocol directly", "Yes", "MySQL Connector/J (used)");
        System.out.println();
        tryLoad("1", "sun.jdbc.odbc.JdbcOdbcDriver");
        tryLoad("4", "com.mysql.cj.jdbc.Driver");
        System.out.println("  Types 2 and 3 need a native library or a middleware server, so they are compared on paper.");

        // ---- 1.2 HikariCP ----
        section("1.2 HikariCP connection pool");
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(URL);
        cfg.setUsername(USER);
        cfg.setPassword(PASS);
        cfg.setPoolName("GracyPool");
        cfg.setMaximumPoolSize(5);              // never more than 5 open connections
        cfg.setMinimumIdle(2);                  // keep 2 ready even when idle
        cfg.setConnectionTimeout(5_000);        // wait max 5 s for a free connection
        cfg.addDataSourceProperty("cachePrepStmts", "true");

        List<Student> loaded = new ArrayList<>();
        try (HikariDataSource ds = new HikariDataSource(cfg)) {
            HikariPoolMXBean pool = ds.getHikariPoolMXBean();
            System.out.println("  Config  : pool=" + cfg.getPoolName() + ", maxPoolSize=" + cfg.getMaximumPoolSize()
                    + ", minIdle=" + cfg.getMinimumIdle() + ", timeout=" + cfg.getConnectionTimeout() + " ms");
            Connection c1 = ds.getConnection(), c2 = ds.getConnection(), c3 = ds.getConnection();
            System.out.println("  Borrowed 3 connections -> active=" + pool.getActiveConnections()
                    + " idle=" + pool.getIdleConnections() + " total=" + pool.getTotalConnections());
            c1.close(); c2.close(); c3.close();                 // close() = give back to the pool
            System.out.println("  close() returns them   -> active=" + pool.getActiveConnections()
                    + " idle=" + pool.getIdleConnections() + " total=" + pool.getTotalConnections() + "  (reused, not destroyed)");

            try (Connection con = ds.getConnection()) {

                // ---- 1.3 Statement: DDL + stored procedures ----
                section("1.3 Statement - create table and stored procedures");
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("DROP TABLE IF EXISTS lab10_student");
                    st.executeUpdate("CREATE TABLE lab10_student (id INT PRIMARY KEY, name VARCHAR(40) NOT NULL,"
                            + " dept VARCHAR(10) NOT NULL, marks INT NOT NULL CHECK (marks BETWEEN 0 AND 100))");
                    st.executeUpdate("DROP PROCEDURE IF EXISTS dept_stats");
                    st.executeUpdate("CREATE PROCEDURE dept_stats(IN p_dept VARCHAR(10), OUT p_count INT, OUT p_avg DECIMAL(5,2))"
                            + " BEGIN SELECT COUNT(*), AVG(marks) INTO p_count, p_avg FROM lab10_student WHERE dept = p_dept; END");
                    st.executeUpdate("DROP PROCEDURE IF EXISTS top_students");
                    st.executeUpdate("CREATE PROCEDURE top_students(IN p_limit INT)"
                            + " BEGIN SELECT id, name, marks FROM lab10_student ORDER BY marks DESC LIMIT p_limit; END");
                }
                System.out.println("  CREATE TABLE lab10_student (id, name, dept, marks)");
                System.out.println("  CREATE PROCEDURE dept_stats(IN dept, OUT count, OUT avg)  and  top_students(IN limit)");

                // ---- 1.4 Batch insert ----
                section("1.4 Batch insert of 10 student records (PreparedStatement + addBatch)");
                Object[][] rows = {
                    {101, "Gracy Sharma", "CSE", 92}, {102, "Aman Gupta", "ECE", 67}, {103, "Riya Patel", "CSE", 81},
                    {104, "Kabir Singh", "ME", 35},   {105, "Sana Khan", "ECE", 88},  {106, "Dev Malhotra", "ME", 74},
                    {107, "Isha Verma", "CSE", 38},   {108, "Arjun Nair", "ECE", 55}, {109, "Meera Joshi", "CSE", 97},
                    {110, "Rohan Das", "ME", 28} };
                con.setAutoCommit(false);
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO lab10_student VALUES (?, ?, ?, ?)")) {
                    for (Object[] r : rows) {
                        ps.setInt(1, (Integer) r[0]);
                        ps.setString(2, (String) r[1]);
                        ps.setString(3, (String) r[2]);
                        ps.setInt(4, (Integer) r[3]);
                        ps.addBatch();
                    }
                    int[] counts = ps.executeBatch();
                    con.commit();
                    System.out.println("  executeBatch() -> " + Arrays.toString(counts) + " -> committed " + counts.length + " rows");
                } finally {
                    con.setAutoCommit(true);
                }

                // ---- 1.5 CRUD with PreparedStatement ----
                section("1.5 CRUD with PreparedStatement (Read / Update / Delete)");
                try (PreparedStatement read = con.prepareStatement("SELECT name, marks FROM lab10_student WHERE id = ?")) {
                    read.setInt(1, 102);
                    try (ResultSet rs = read.executeQuery()) {
                        if (rs.next()) System.out.println("  READ   id 102            -> " + rs.getString("name") + ", " + rs.getInt("marks") + " marks");
                    }
                }
                try (PreparedStatement upd = con.prepareStatement("UPDATE lab10_student SET marks = ? WHERE id = ?")) {
                    upd.setInt(1, 72);
                    upd.setInt(2, 102);
                    System.out.println("  UPDATE id 102 marks = 72  -> " + upd.executeUpdate() + " row updated");
                }
                try (PreparedStatement del = con.prepareStatement("DELETE FROM lab10_student WHERE id = ?")) {
                    del.setInt(1, 106);
                    System.out.println("  DELETE id 106             -> " + del.executeUpdate() + " row deleted");
                }

                // ---- 1.6 CallableStatement ----
                section("1.6 CallableStatement - calling stored procedures");
                try (CallableStatement cs = con.prepareCall("{call dept_stats(?, ?, ?)}")) {
                    for (String d : new String[] {"CSE", "ECE", "ME"}) {
                        cs.setString(1, d);                                  // IN
                        cs.registerOutParameter(2, Types.INTEGER);           // OUT
                        cs.registerOutParameter(3, Types.DECIMAL);           // OUT
                        cs.execute();
                        System.out.printf("  {call dept_stats(%-5s, ?, ?)} -> count = %d, average = %s%n",
                                "'" + d + "'", cs.getInt(2), cs.getBigDecimal(3));
                    }
                }
                try (CallableStatement cs = con.prepareCall("{call top_students(?)}")) {
                    cs.setInt(1, 3);
                    try (ResultSet rs = cs.executeQuery()) {
                        System.out.println("  {call top_students(3)} returns a ResultSet:");
                        printResultSet(rs);
                    }
                }

                // ---- 1.7 Transaction with savepoint ----
                section("1.7 Transaction - COMMIT, SAVEPOINT and ROLLBACK");
                con.setAutoCommit(false);
                Savepoint afterGrace = null;
                try (Statement st = con.createStatement()) {
                    int graced = st.executeUpdate("UPDATE lab10_student SET marks = marks + 5 WHERE marks BETWEEN 35 AND 39");
                    System.out.println("  step 1  +5 grace marks to students with 35-39    -> " + graced + " row(s)");
                    afterGrace = con.setSavepoint("after_grace");
                    System.out.println("          SAVEPOINT after_grace created");
                    st.executeUpdate("INSERT INTO lab10_student VALUES (111, 'Tara Bose', 'ECE', 77)");
                    System.out.println("  step 2  INSERT id 111 Tara Bose                  -> ok (not committed yet)");
                    st.executeUpdate("INSERT INTO lab10_student VALUES (101, 'Duplicate', 'CSE', 50)");
                    System.out.println("  step 3  INSERT id 101 again                      -> ok");
                } catch (SQLException e) {
                    System.out.println("  step 3  INSERT id 101 again                      -> FAILED: " + e.getMessage());
                    con.rollback(afterGrace);
                    System.out.println("          ROLLBACK TO SAVEPOINT after_grace        -> steps 2 and 3 undone, step 1 kept");
                }
                con.commit();
                con.setAutoCommit(true);
                System.out.println("          COMMIT                                   -> step 1 saved permanently");
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM lab10_student WHERE id = 111")) {
                    rs.next();
                    System.out.println("  Check: rows with id 111 after commit = " + rs.getInt(1));
                }

                // ---- 1.8 ResultSet display + load into records ----
                section("1.8 ResultSet - final contents of lab10_student");
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, name, dept, marks FROM lab10_student ORDER BY id")) {
                    List<String[]> table = printResultSet(rs);
                    for (String[] r : table) {                       // ResultSet rows -> Student records
                        int id = Integer.parseInt(r[0]);
                        loaded.add(new Student(id, r[1], Integer.parseInt(r[3])));
                        DEPT_CODE.put(id, r[2]);
                    }
                }
            }
        }   // pool closed here -> HikariCP logs the shutdown
        return loaded;
    }

    // ########################################################################
    //  PART 2 - Collectors, flatMap, reduce, Optional
    // ########################################################################
    record Department(String name, String hod) {}

    static final Map<String, Department> DEPARTMENTS = Map.of(
            "CSE", new Department("Computer Science", "Dr. Mehta"),
            "ECE", new Department("Electronics", null));          // ME is missing on purpose

    static final Map<Integer, List<String>> COURSES = Map.of(
            101, List.of("Java", "DBMS", "Maths"), 102, List.of("Signals", "Maths"),
            103, List.of("Java", "Networks"),      105, List.of("Signals", "VLSI", "Maths"),
            109, List.of("Java", "DBMS", "AI"),    110, List.of("Mechanics", "Maths"));

    static void partTwoStreams(List<Student> students) {
        part("PART 2 - COLLECTORS, flatMap, reduce AND OPTIONAL (on the records loaded from MySQL)");

        Map<Character, List<String>> byGrade = students.stream()
                .collect(Collectors.groupingBy(Student::grade, TreeMap::new,
                         Collectors.mapping(Lab10::firstName, Collectors.toList())));
        System.out.println("  groupingBy(grade)     : " + byGrade);

        Map<Boolean, List<String>> passFail = students.stream()
                .collect(Collectors.partitioningBy(s -> s.marks() >= 40,
                         Collectors.mapping(Lab10::firstName, Collectors.toList())));
        System.out.println("  partitioningBy(>= 40) : PASS " + passFail.get(true) + "  FAIL " + passFail.get(false));

        String names = students.stream().map(Lab10::firstName).sorted()
                .collect(Collectors.joining(", "));
        System.out.println("  joining(\", \")         : \"" + names + "\"");

        List<List<String>> courseLists = students.stream()
                .map(s -> COURSES.getOrDefault(s.id(), List.of())).collect(Collectors.toList());
        List<String> allEnrollments = courseLists.stream()
                .flatMap(List::stream)                                     // List<List<String>> -> Stream<String>
                .collect(Collectors.toList());
        List<String> uniqueCourses = allEnrollments.stream().distinct().sorted().collect(Collectors.toList());
        System.out.println("  flatMap(course lists) : " + courseLists.get(0) + " + " + courseLists.get(1) + " + ...  ("
                + courseLists.size() + " lists)");
        System.out.println("                       -> " + allEnrollments.size() + " enrollments, "
                + uniqueCourses.size() + " unique: " + uniqueCourses);

        int total = students.stream().map(Student::marks).reduce(0, Integer::sum);
        Optional<Student> topper = students.stream().reduce((a, b) -> a.marks() >= b.marks() ? a : b);
        System.out.println("  reduce(0, Integer::sum) total marks = " + total
                + "  |  reduce(max) topper = " + topper.map(Student::name).orElse("-"));

        System.out.println();
        System.out.println("  Optional chain: find(id) -> dept code -> Department -> name / hod");
        for (int id : new int[] {101, 104, 999}) {
            String dept = findStudent(students, id)
                    .map(s -> DEPT_CODE.get(s.id()))                     // Optional<String>
                    .map(DEPARTMENTS::get)                                // Optional<Department>
                    .map(Department::name)                                // Optional<String>
                    .orElse("<not available>");
            System.out.printf("    id %-4d -> department = %s%n", id, dept);
        }
        String hod = findStudent(students, 102).map(s -> DEPT_CODE.get(s.id()))
                .map(DEPARTMENTS::get).map(Department::hod).orElse("HOD not appointed yet");
        System.out.println("    id 102  -> hod        = " + hod + "   (no null checks, no NullPointerException)");
    }

    static String firstName(Student s) { return s.name().split(" ")[0]; }

    static Optional<Student> findStudent(List<Student> list, int id) {
        return list.stream().filter(s -> s.id() == id).findFirst();
    }

    // ########################################################################
    //  PART 3 - Records and Sealed classes
    // ########################################################################
    static void partThreeRecordsAndSealed() throws Exception {
        part("PART 3 - RECORDS AND SEALED CLASSES");

        section("3.1 record Student(int id, String name, int marks) - immutability");
        Student s1 = new Student(101, "Gracy Sharma", 92);
        Student s2 = new Student(101, "Gracy Sharma", 92);
        System.out.println("  toString()           : " + s1);
        System.out.println("  accessors            : s1.id()=" + s1.id() + ", s1.name()=" + s1.name() + ", s1.marks()=" + s1.marks());
        System.out.println("  s1.equals(s2)        : " + s1.equals(s2) + "   s1 == s2 : " + (s1 == s2)
                + "   same hashCode : " + (s1.hashCode() == s2.hashCode()));
        StringJoiner fields = new StringJoiner(", ");
        for (Field f : Student.class.getDeclaredFields()) {
            fields.add(Modifier.toString(f.getModifiers()) + " " + f.getType().getSimpleName() + " " + f.getName());
        }
        System.out.println("  generated fields     : " + fields);
        long setters = Arrays.stream(Student.class.getMethods()).filter(m -> m.getName().startsWith("set")).count();
        System.out.println("  setter methods       : " + setters);
        Student s3 = s1.withMarks(97);
        System.out.println("  s1.withMarks(97)     : new " + s3 + ", s1 still has " + s1.marks());
        try {
            new Student(102, "Aman Gupta", 150);
        } catch (IllegalArgumentException e) {
            System.out.println("  new Student(.., 150) : IllegalArgumentException -> " + e.getMessage());
        }
        System.out.println("  add a setter inside the record?");
        System.out.println("    " + compileError(
                "record Student(int id, String name, int marks) {\n"
              + "    void setMarks(int m) { this.marks = m; } }"));

        section("3.2 sealed abstract class Shape permits Circle, Rectangle, Triangle");
        System.out.println("  Shape.isSealed()      : " + Shape.class.isSealed());
        StringJoiner permitted = new StringJoiner(", ");
        for (Class<?> c : Shape.class.getPermittedSubclasses()) {
            String kind = Modifier.isFinal(c.getModifiers()) ? "final" : c.isSealed() ? "sealed" : "non-sealed";
            permitted.add(kind + " " + c.getSimpleName());
        }
        System.out.println("  permitted subclasses  : " + permitted);
        Shape[] shapes = { new Circle(2), new Rectangle(4, 3), new Triangle(3, 4, 5), new Square(2.5) };
        for (Shape s : shapes) {
            System.out.printf("  %-10s area = %6.2f  -> %s%n", s.getClass().getSimpleName(), s.area(), describe(s));
        }
        System.out.println("  final class Hexagon extends Shape ?");
        System.out.println("    " + compileError(
                "sealed abstract class Shape permits Circle {}\n"
              + "final class Circle extends Shape {}\n"
              + "final class Hexagon extends Shape {}"));
    }

    /** instanceof pattern matching - every permitted subtype is handled. */
    static String describe(Shape s) {
        if (s instanceof Circle c)         return "round shape, radius " + c.radius();
        else if (s instanceof Square sq)   return "Square via non-sealed Rectangle, side " + sq.width();
        else if (s instanceof Rectangle r) return "four right angles, " + r.width() + " x " + r.height();
        else if (s instanceof Triangle t)  return "three sides, perimeter " + t.perimeter();
        throw new IllegalStateException("impossible - Shape is sealed");
    }

    // ########################################################################
    //  HELPERS
    // ########################################################################
    static void driverRow(String t, String n, String p, String j, String e) {
        System.out.printf("  %-4s | %-17s | %-32s | %-4s | %s%n", t, n, p, j, e);
    }

    static void tryLoad(String type, String className) {
        try {
            Class<?> cls = Class.forName(className);
            Driver d = (Driver) cls.getDeclaredConstructor().newInstance();
            System.out.printf("  Type %s : Class.forName(\"%s\")     -> LOADED, Connector/J %d.%d (pure Java jar)%n",
                    type, className, d.getMajorVersion(), d.getMinorVersion());
        } catch (ClassNotFoundException e) {
            System.out.printf("  Type %s : Class.forName(\"%s\") -> ClassNotFoundException (bridge removed)%n", type, className);
        } catch (ReflectiveOperationException e) {
            System.out.println("  Type " + type + " : could not create driver: " + e);
        }
    }

    /** Prints any ResultSet as a MySQL-style table and returns the rows. */
    static List<String[]> printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        String[] head = new String[cols];
        int[] width = new int[cols];
        for (int c = 1; c <= cols; c++) { head[c - 1] = meta.getColumnLabel(c); width[c - 1] = head[c - 1].length(); }
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] r = new String[cols];
            for (int c = 1; c <= cols; c++) { r[c - 1] = rs.getString(c); width[c - 1] = Math.max(width[c - 1], r[c - 1].length()); }
            rows.add(r);
        }
        StringBuilder sep = new StringBuilder("  +");
        for (int w : width) sep.append("-".repeat(w + 2)).append('+');
        System.out.println(sep);
        System.out.println(row(head, width));
        System.out.println(sep);
        for (String[] r : rows) System.out.println(row(r, width));
        System.out.println(sep);
        return rows;
    }

    static String row(String[] v, int[] width) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < v.length; i++) sb.append(' ').append(String.format("%-" + width[i] + "s", v[i])).append(" |");
        return sb.toString();
    }

    /** Compiles a snippet in memory and returns the compiler's first error. */
    static String compileError(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///Snippet.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignore) { return source; }
        };
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        boolean ok = compiler.getTask(null, null, diag, List.of("-d", System.getProperty("java.io.tmpdir")),
                null, List.of(file)).call();
        if (ok) return "compiled (unexpected)";
        return "compile ERROR: " + diag.getDiagnostics().get(0).getMessage(null).split("\n")[0];
    }

    static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    static void part(String t) {
        System.out.println();
        System.out.println("#".repeat(86));
        System.out.println("#  " + t);
        System.out.println("#".repeat(86));
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(86)); }
}

// ============================================================================
//  RECORD - an immutable data carrier (Java 16+)
// ============================================================================
/** Java generates: private final fields, constructor, accessors, equals, hashCode, toString. */
record Student(int id, String name, int marks) {

    /** compact constructor - runs validation before the fields are assigned */
    Student {
        if (marks < 0 || marks > 100) throw new IllegalArgumentException("marks must be 0-100, got " + marks);
        Objects.requireNonNull(name, "name");
    }

    char grade() {
        if (marks >= 90) return 'A';
        if (marks >= 75) return 'B';
        if (marks >= 60) return 'C';
        if (marks >= 40) return 'D';
        return 'F';
    }

    /** "changing" a record means creating a new one */
    Student withMarks(int newMarks) { return new Student(id, name, newMarks); }
}

// ============================================================================
//  SEALED HIERARCHY - only the listed classes may extend Shape (Java 17)
// ============================================================================
sealed abstract class Shape permits Circle, Rectangle, Triangle {
    abstract double area();
}

final class Circle extends Shape {                 // final: nobody can extend Circle
    private final double radius;
    Circle(double radius) { this.radius = radius; }
    double radius() { return radius; }
    double area() { return Math.PI * radius * radius; }
}

non-sealed class Rectangle extends Shape {         // non-sealed: open again for extension
    private final double width, height;
    Rectangle(double width, double height) { this.width = width; this.height = height; }
    double width()  { return width; }
    double height() { return height; }
    double area() { return width * height; }
}

class Square extends Rectangle {                   // allowed because Rectangle is non-sealed
    Square(double side) { super(side, side); }
}

final class Triangle extends Shape {
    private final double a, b, c;
    Triangle(double a, double b, double c) { this.a = a; this.b = b; this.c = c; }
    double perimeter() { return a + b + c; }
    double area() {                                // Heron's formula
        double s = perimeter() / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
}
