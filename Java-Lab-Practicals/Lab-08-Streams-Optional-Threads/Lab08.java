/*
 * ============================================================================
 *  LAB 08 - Java 8 Features & Multithreading
 * ============================================================================
 *  Aim : Write programs to
 *        (1) use Lambda expressions and Stream API to filter, sort and group
 *            student objects using groupingBy and partitioningBy
 *        (2) demonstrate Optional to handle null safely
 *        (3) simulate producer-consumer using the Thread class and the
 *            Runnable interface
 *        (4) use a synchronized block to prevent race conditions on a shared
 *            bank account balance
 *
 *  CO4 : Apply Java 8 features and implement multithreaded programs
 *  Run : java Lab08.java
 * ============================================================================
 */
import java.util.*;
import java.util.stream.Collectors;

public class Lab08 {

    public static void main(String[] args) throws InterruptedException {
        line('=');
        System.out.println("  LAB 08 : LAMBDAS, STREAMS, OPTIONAL & MULTITHREADING");
        line('=');

        List<Student> students = List.of(
                new Student("Gracy", "CSE", 92, "Dr. Mehta"),
                new Student("Aman", "ECE", 67, null),
                new Student("Riya", "CSE", 81, "Dr. Rao"),
                new Student("Kabir", "ME", 35, null),
                new Student("Sana", "ECE", 88, "Dr. Iyer"),
                new Student("Dev", "ME", 74, "Dr. Khan"),
                new Student("Isha", "CSE", 38, null),
                new Student("Arjun", "ECE", 55, "Dr. Iyer"));

        // ===================================================================
        // (1) LAMBDAS + STREAM API
        // ===================================================================
        section("1. Lambdas & Stream API - filter, sort, groupingBy, partitioningBy");
        List<String> toppers = students.stream()
                .filter(s -> s.getMarks() >= 75)                                  // lambda predicate
                .sorted(Comparator.comparingInt(Student::getMarks).reversed())   // sort high -> low
                .map(s -> s.getName() + "(" + s.getMarks() + ")")
                .collect(Collectors.toList());
        System.out.println("  filter >= 75, sorted desc : " + toppers);

        Map<String, List<String>> byDept = students.stream()
                .collect(Collectors.groupingBy(Student::getDept, TreeMap::new,
                         Collectors.mapping(Student::getName, Collectors.toList())));
        System.out.println("  groupingBy(dept)          : " + byDept);

        Map<String, Double> avgByDept = students.stream()
                .collect(Collectors.groupingBy(Student::getDept, TreeMap::new,
                         Collectors.averagingInt(Student::getMarks)));
        System.out.print("  average marks per dept    : ");
        avgByDept.forEach((dept, avg) -> System.out.printf("%s=%.1f  ", dept, avg));
        System.out.println();

        Map<Boolean, List<String>> passFail = students.stream()
                .collect(Collectors.partitioningBy(s -> s.getMarks() >= 40,
                         Collectors.mapping(Student::getName, Collectors.toList())));
        System.out.println("  partitioningBy(>= 40)     : PASS " + passFail.get(true) + "  FAIL " + passFail.get(false));

        // ===================================================================
        // (2) OPTIONAL
        // ===================================================================
        section("2. Optional - handling null safely");
        Optional<Student> found = findByName(students, "Gracy");
        Optional<Student> missing = findByName(students, "Zara");
        System.out.println("  findByName(\"Gracy\").isPresent()            = " + found.isPresent());
        found.ifPresent(s -> System.out.println("  ifPresent -> " + s.getName() + " is in " + s.getDept()));
        System.out.println("  findByName(\"Zara\").map(getDept).orElse(..) = "
                + missing.map(Student::getDept).orElse("Student not found"));
        System.out.println("  orElseGet(() -> new guest id)              = "
                + missing.map(Student::getName).orElseGet(() -> "Guest#" + (students.size() + 1)));   // lazy default

        Student aman = findByName(students, "Aman").get();
        try {
            System.out.println("  aman.getMentor().length() = " + aman.getMentor().length());
        } catch (NullPointerException e) {
            System.out.println("  WITHOUT Optional: aman.getMentor().length() -> NullPointerException!");
        }
        String mentor = Optional.ofNullable(aman.getMentor()).orElse("No mentor assigned");
        System.out.println("  WITH Optional   : ofNullable(aman.getMentor()).orElse(..) -> " + mentor);
        try {
            missing.orElseThrow(() -> new NoSuchElementException("Zara is not enrolled"));
        } catch (NoSuchElementException e) {
            System.out.println("  orElseThrow(..) -> NoSuchElementException: " + e.getMessage());
        }

        // ===================================================================
        // (3) PRODUCER - CONSUMER  (wait / notify)
        // ===================================================================
        section("3. Producer-Consumer: Runnable producer + Thread consumer (buffer size 2)");
        SharedBuffer buffer = new SharedBuffer(2);
        Thread producer = new Thread(new Producer(buffer, 5), "Producer");   // via Runnable
        Consumer consumer = new Consumer(buffer, 5);                         // via extends Thread
        producer.start();
        consumer.start();
        producer.join();                                                     // wait for both to finish
        consumer.join();
        System.out.println("  Both threads finished. Items left in buffer: " + buffer.size());

        // ===================================================================
        // (4) RACE CONDITION + synchronized block
        // ===================================================================
        section("4. Race condition on a shared bank balance & the synchronized fix");
        int threads = 4, depositsEach = 50_000;
        System.out.printf("  %d threads x %,d deposits of Rs.1 each -> expected balance Rs.%,d%n",
                threads, depositsEach, threads * depositsEach);
        for (int run = 1; run <= 2; run++) {
            BankAccount unsafe = new BankAccount();
            runDeposits(unsafe, threads, depositsEach, false);
            BankAccount safe = new BankAccount();
            runDeposits(safe, threads, depositsEach, true);
            System.out.printf("  Run %d | WITHOUT sync: Rs.%,8d (lost %,6d updates) | WITH synchronized: Rs.%,d%n",
                    run, unsafe.getBalance(), threads * depositsEach - unsafe.getBalance(), safe.getBalance());
        }
        System.out.println("  => synchronized(lock) lets only ONE thread update the balance at a time.");

        line('=');
        System.out.println("  Lab 08 completed successfully");
        line('=');
    }

    static Optional<Student> findByName(List<Student> list, String name) {
        return list.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    static void runDeposits(BankAccount acc, int threads, int each, boolean useSync) throws InterruptedException {
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Runnable job = () -> {                                          // lambda as Runnable
                for (int i = 0; i < each; i++) {
                    if (useSync) acc.depositSafe(1); else acc.depositUnsafe(1);
                }
            };
            workers.add(new Thread(job));
        }
        for (Thread w : workers) w.start();
        for (Thread w : workers) w.join();
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

// ============================================================================
//  MODEL
// ============================================================================
class Student {
    private final String name, dept, mentor;
    private final int marks;

    Student(String name, String dept, int marks, String mentor) {
        this.name = name; this.dept = dept; this.marks = marks; this.mentor = mentor;
    }

    String getName()   { return name; }
    String getDept()   { return dept; }
    int getMarks()     { return marks; }
    String getMentor() { return mentor; }          // may be null!
}

// ============================================================================
//  PRODUCER - CONSUMER
// ============================================================================
class SharedBuffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;

    SharedBuffer(int capacity) { this.capacity = capacity; }

    synchronized void put(int item) throws InterruptedException {
        while (queue.size() == capacity) {
            System.out.println("    [Producer] buffer FULL  -> waiting...");
            wait();                                 // releases the lock and sleeps
        }
        queue.add(item);
        System.out.println("    [Producer] produced item " + item + "   buffer=" + queue);
        notifyAll();                                // wake up the consumer
    }

    synchronized int take() throws InterruptedException {
        while (queue.isEmpty()) {
            System.out.println("    [Consumer] buffer EMPTY -> waiting...");
            wait();
        }
        int item = queue.poll();
        System.out.println("    [Consumer] consumed item " + item + "   buffer=" + queue);
        notifyAll();                                // wake up the producer
        return item;
    }

    synchronized int size() { return queue.size(); }
}

/** Producer created by implementing Runnable. */
class Producer implements Runnable {
    private final SharedBuffer buffer;
    private final int count;

    Producer(SharedBuffer buffer, int count) { this.buffer = buffer; this.count = count; }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= count; i++) {
                buffer.put(i);
                Thread.sleep(40);                   // producer is fast
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/** Consumer created by extending Thread. */
class Consumer extends Thread {
    private final SharedBuffer buffer;
    private final int count;

    Consumer(SharedBuffer buffer, int count) {
        super("Consumer");
        this.buffer = buffer;
        this.count = count;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(150);                      // start late so the buffer fills up
            for (int i = 1; i <= count; i++) {
                buffer.take();
                Thread.sleep(120);                  // consumer is slow
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// ============================================================================
//  SHARED BANK ACCOUNT
// ============================================================================
class BankAccount {
    private int balance = 0;
    private final Object lock = new Object();

    /** NOT thread-safe: read-modify-write can interleave between threads. */
    void depositUnsafe(int amount) {
        balance = balance + amount;
    }

    /** Thread-safe: the synchronized block allows one thread at a time. */
    void depositSafe(int amount) {
        synchronized (lock) {
            balance = balance + amount;
        }
    }

    int getBalance() {
        synchronized (lock) { return balance; }
    }
}
