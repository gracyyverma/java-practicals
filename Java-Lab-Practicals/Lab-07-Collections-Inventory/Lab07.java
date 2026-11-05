/*
 * ============================================================================
 *  LAB 07 - Collections Framework (Inventory Management System)
 * ============================================================================
 *  Aim : Implement a simple inventory management system using
 *        (1) ArrayList to store and sort products by price using Comparator
 *        (2) HashMap to store product ID-name pairs with lookup, update, delete
 *        (3) HashSet to remove duplicate entries
 *        (4) PriorityQueue to process orders by priority
 *        Demonstrate Iterator and enhanced for-loop across all collections.
 *
 *  CO4 : Utilize Java Collections Framework for data management
 *  Run : java Lab07.java
 * ============================================================================
 */
import java.util.*;

public class Lab07 {

    public static void main(String[] args) {
        line('=');
        System.out.println("  LAB 07 : INVENTORY MANAGEMENT - JAVA COLLECTIONS FRAMEWORK");
        line('=');

        // ===================================================================
        // (1) ArrayList + Comparator
        // ===================================================================
        section("1. ArrayList - store products and sort by price using Comparator");
        List<Product> inventory = new ArrayList<>();
        inventory.add(new Product(101, "Laptop", 55000, 5));
        inventory.add(new Product(102, "Mouse", 499, 40));
        inventory.add(new Product(103, "Monitor", 12500, 0));
        inventory.add(new Product(104, "Keyboard", 1299, 25));
        inventory.add(new Product(105, "Webcam", 2999, 0));
        inventory.add(new Product(106, "Headphones", 1999, 12));

        // Comparator written as a method reference / lambda
        inventory.sort(Comparator.comparingDouble(Product::getPrice));
        System.out.println("  Sorted by price (LOW -> HIGH) using Comparator.comparingDouble():");
        Product.printHeader();
        for (Product p : inventory) System.out.println("  " + p);          // enhanced for-loop

        // Same idea written as a classic anonymous Comparator class
        inventory.sort(new Comparator<Product>() {
            @Override
            public int compare(Product a, Product b) {
                return Double.compare(b.getPrice(), a.getPrice());          // descending
            }
        });
        System.out.print("  Sorted HIGH -> LOW (anonymous Comparator): ");
        StringJoiner sj = new StringJoiner(" > ");
        for (Product p : inventory) sj.add(p.getName());
        System.out.println(sj);

        // ===================================================================
        // (2) HashMap  ID -> name   (lookup / update / delete)
        // ===================================================================
        section("2. HashMap<Integer, String> - lookup, update, delete");
        Map<Integer, String> catalog = new HashMap<>();
        for (Product p : inventory) catalog.put(p.getId(), p.getName());
        System.out.println("  Catalog          : " + new TreeMap<>(catalog));
        System.out.println("  LOOKUP  get(104) : " + catalog.get(104));
        System.out.println("  LOOKUP  get(999) : " + catalog.getOrDefault(999, "<no such product>"));
        catalog.put(102, "Wireless Mouse");                                  // update existing key
        System.out.println("  UPDATE  put(102, \"Wireless Mouse\") -> " + catalog.get(102));
        String removed = catalog.remove(105);                               // delete
        System.out.println("  DELETE  remove(105) removed \"" + removed + "\", containsKey(105) = " + catalog.containsKey(105));
        System.out.println("  Catalog now      : " + new TreeMap<>(catalog) + "  size = " + catalog.size());

        // ===================================================================
        // (3) HashSet - remove duplicates
        // ===================================================================
        section("3. HashSet - remove duplicate stock entries");
        List<Product> incomingStock = List.of(
                new Product(201, "USB Cable", 199, 10), new Product(202, "Pen Drive", 649, 8),
                new Product(201, "USB Cable", 199, 10), new Product(203, "HDMI Cable", 349, 6),
                new Product(202, "Pen Drive", 649, 8),  new Product(201, "USB Cable", 199, 10));
        Set<Product> unique = new HashSet<>(incomingStock);                 // uses equals() + hashCode()
        System.out.println("  Entries scanned (List)  : " + incomingStock.size());
        System.out.println("  Unique entries (HashSet): " + unique.size());
        for (Product p : unique) System.out.println("    -> " + p.getId() + " " + p.getName());
        System.out.println("  add() of a duplicate returns: " + unique.add(new Product(203, "HDMI Cable", 349, 6)));

        // ===================================================================
        // (4) PriorityQueue - process orders by priority
        // ===================================================================
        section("4. PriorityQueue - orders processed by priority (1 = most urgent)");
        PriorityQueue<Order> orders = new PriorityQueue<>(
                Comparator.comparingInt(Order::priority).thenComparingInt(Order::id));
        orders.add(new Order(5001, "Laptop", 3));
        orders.add(new Order(5002, "Mouse", 1));
        orders.add(new Order(5003, "Monitor", 2));
        orders.add(new Order(5004, "Keyboard", 1));
        orders.add(new Order(5005, "Headphones", 3));
        System.out.println("  Added in order : 5001(P3) 5002(P1) 5003(P2) 5004(P1) 5005(P3)");
        System.out.println("  peek()         : " + orders.peek());
        System.out.print("  Iterator order : ");                     // NOT sorted - heap order
        Iterator<Order> oit = orders.iterator();
        while (oit.hasNext()) System.out.print(oit.next().id() + " ");
        System.out.println(" <- internal heap layout, not priority order!");
        System.out.println("  poll() order   :");
        int step = 1;
        while (!orders.isEmpty()) System.out.println("    " + step++ + ". " + orders.poll());

        // ===================================================================
        // (5) Iterator vs enhanced for-loop across collections
        // ===================================================================
        section("5. Iterator and enhanced for-loop across the collections");
        // Removing while looping: enhanced for-loop FAILS, Iterator.remove() is safe
        List<Product> scratch = new ArrayList<>(inventory);                 // try it on a copy
        try {
            for (Product p : scratch) if (p.getQty() == 0) scratch.remove(p);
        } catch (ConcurrentModificationException e) {
            System.out.println("  ArrayList + for-each + remove()  -> ConcurrentModificationException!");
        }
        Iterator<Product> it = inventory.iterator();
        while (it.hasNext()) {
            Product p = it.next();
            if (p.getQty() == 0) {
                it.remove();                                                // safe removal
                System.out.println("  ArrayList + Iterator.remove()    -> removed out-of-stock " + p.getName());
            }
        }
        System.out.println("  In-stock products now            -> " + inventory.size());

        System.out.print("  HashMap  (Iterator on entrySet)  -> ");
        Iterator<Map.Entry<Integer, String>> mit = new TreeMap<>(catalog).entrySet().iterator();
        while (mit.hasNext()) { Map.Entry<Integer, String> e = mit.next(); System.out.print(e.getKey() + "=" + e.getValue() + "  "); }
        System.out.println();

        System.out.print("  HashSet  (enhanced for-loop)     -> ");
        for (Product p : unique) System.out.print(p.getName() + "  ");
        System.out.println();

        line('=');
        System.out.println("  Lab 07 completed successfully");
        line('=');
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

/** Product - equals() and hashCode() let HashSet detect duplicates. */
class Product {
    private final int id;
    private final String name;
    private final double price;
    private final int qty;

    Product(int id, String name, double price, int qty) {
        this.id = id; this.name = name; this.price = price; this.qty = qty;
    }

    int getId()       { return id; }
    String getName()  { return name; }
    double getPrice() { return price; }
    int getQty()      { return qty; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product p = (Product) o;
        return id == p.id && name.equals(p.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    static void printHeader() {
        System.out.printf("  %-5s | %-11s | %10s | %s%n", "ID", "Product", "Price (Rs)", "Qty");
        System.out.println("  " + "-".repeat(6) + "+" + "-".repeat(13) + "+" + "-".repeat(12) + "+" + "-".repeat(5));
    }

    @Override
    public String toString() {
        return String.format("%-5d | %-11s | %,10.2f | %3d", id, name, price, qty);
    }
}

/** A small immutable order. Lower priority number = more urgent. */
class Order {
    private final int id;
    private final String item;
    private final int priority;

    Order(int id, String item, int priority) { this.id = id; this.item = item; this.priority = priority; }

    int id()       { return id; }
    int priority() { return priority; }

    @Override
    public String toString() {
        String label = priority == 1 ? "EXPRESS" : priority == 2 ? "STANDARD" : "ECONOMY";
        return "Order " + id + " [P" + priority + " " + label + "] " + item;
    }
}
