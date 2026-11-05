/*
 * ============================================================================
 *  LAB 04 - Interfaces vs Abstract Classes (Shape & Polygon)
 * ============================================================================
 *  Aim : Create an interface Shape with abstract methods area() and
 *        perimeter(). Implement it in classes Circle, Rectangle and Triangle.
 *        Create an abstract class Polygon with a concrete method describe().
 *        Demonstrate multiple interface implementation and compare behaviour
 *        with abstract classes through a driver class.
 *
 *  CO2 : Apply interfaces and abstract classes for modular design
 *  Run : java Lab04.java
 * ============================================================================
 */
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/** DRIVER CLASS - creates the objects and exercises every feature. */
public class Lab04 {

    public static void main(String[] args) {
        line('=');
        System.out.println("  LAB 04 : SHAPE INTERFACE  vs  POLYGON ABSTRACT CLASS");
        line('=');

        Circle circle = new Circle(3.5);
        Rectangle rect = new Rectangle(6, 4);
        Triangle tri = new Triangle(3, 4, 5);

        // ---- 1. Polymorphism through the interface ----
        section("1. Same interface, different implementations  (Shape[] array)");
        Shape[] shapes = { circle, rect, tri };
        for (Shape s : shapes) {
            System.out.println("  " + s.summary());
        }

        // ---- 2. Abstract class with concrete method ----
        section("2. Abstract class Polygon -> concrete describe() is INHERITED");
        Polygon[] polygons = { rect, tri };        // Circle is NOT a Polygon
        for (Polygon p : polygons) {
            System.out.println("  " + p.describe());
        }
        System.out.println("  (Circle is a Shape but not a Polygon: it has no straight sides.)");

        // ---- 3. Multiple interface implementation ----
        section("3. Multiple interface implementation");
        for (Shape s : shapes) {
            String ifaces = Arrays.stream(s.getClass().getInterfaces())
                    .map(Class::getSimpleName).collect(Collectors.joining(", "));
            String parent = s.getClass().getSuperclass().getSimpleName();
            System.out.printf("  %-10s extends %-7s implements %s%n", s.getClass().getSimpleName(), parent, ifaces);
        }
        System.out.println();
        for (Shape s : shapes) {
            if (s instanceof Drawable d) d.draw();        // only if it is Drawable
        }
        System.out.println();
        System.out.println("  Resizable objects scaled by 2x:");
        for (Shape s : shapes) {
            if (s instanceof Resizable r) {
                double before = s.area();
                r.resize(2);
                System.out.printf("    %-10s area %7.2f -> %7.2f  (x%.0f, area grows by factor^2)%n",
                        s.name(), before, s.area(), s.area() / before);
            } else {
                System.out.printf("    %-10s is not Resizable -> skipped%n", s.name());
            }
        }

        // ---- 4. Interface vs Abstract class - compared at runtime ----
        section("4. Interface vs Abstract class - checked at runtime");
        System.out.println("  Shape.isInterface()               = " + Shape.class.isInterface());
        System.out.println("  Polygon is abstract?              = " + Modifier.isAbstract(Polygon.class.getModifiers()));
        System.out.println("  Fields declared in Shape          = " + Shape.class.getDeclaredFields().length
                + "   | in Polygon = " + Polygon.class.getDeclaredFields().length + " (sides, name)");
        System.out.println("  Constructors in Shape             = " + Shape.class.getDeclaredConstructors().length
                + "   | in Polygon = " + Polygon.class.getDeclaredConstructors().length);
        System.out.println("  rect instanceof Shape / Polygon   = " + (rect instanceof Shape) + " / " + (rect instanceof Polygon));
        System.out.println();
        System.out.printf("  %-24s| %-26s| %s%n", "Feature", "Interface (Shape)", "Abstract class (Polygon)");
        System.out.println("  " + "-".repeat(24) + "+" + "-".repeat(27) + "+" + "-".repeat(26));
        row("Keyword to use", "implements", "extends");
        row("How many per class?", "many (Rectangle uses 3)", "only one");
        row("Instance fields / state", "no (constants only)", "yes (sides, name)");
        row("Constructors", "no", "yes");
        row("Method bodies", "default / static only", "concrete + abstract");
        row("Can do 'new' directly?", "no", "no");
        row("Best for", "a CAPABILITY (can-do)", "a FAMILY (is-a) + shared code");

        line('=');
        System.out.println("  Lab 04 completed successfully");
        line('=');
    }

    static void row(String a, String b, String c) { System.out.printf("  %-24s| %-26s| %s%n", a, b, c); }
    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

// ============================================================================
//  INTERFACES  - pure contracts (what an object CAN DO)
// ============================================================================
interface Shape {
    double area();                       // abstract (implicitly public abstract)
    double perimeter();                  // abstract
    String name();

    /** default method - interfaces may carry a body since Java 8 */
    default String summary() {
        return String.format("%-10s area = %8.2f   perimeter = %7.2f", name(), area(), perimeter());
    }
}

interface Drawable {
    void draw();
}

interface Resizable {
    void resize(double factor);
}

// ============================================================================
//  ABSTRACT CLASS - partial implementation shared by a family (what it IS)
// ============================================================================
abstract class Polygon {
    protected final int sides;           // abstract classes CAN hold state
    protected final String name;

    Polygon(int sides, String name) {    // ...and CAN have constructors
        this.sides = sides;
        this.name = name;
    }

    /** concrete method - every Polygon gets this for free */
    String describe() {
        int angleSum = (sides - 2) * 180;
        return String.format("%-9s : polygon with %d sides, interior angles add up to %d deg", name, sides, angleSum);
    }
}

// ============================================================================
//  CONCRETE CLASSES
// ============================================================================

/** Circle: implements two interfaces, no abstract parent. */
class Circle implements Shape, Drawable {
    private final double radius;

    Circle(double radius) { this.radius = radius; }

    public double area()      { return Math.PI * radius * radius; }
    public double perimeter() { return 2 * Math.PI * radius; }
    public String name()      { return "Circle"; }
    public void draw()        { System.out.println("  Drawing  ( O )  Circle    r = " + radius); }
}

/** Rectangle: extends ONE abstract class + implements THREE interfaces. */
class Rectangle extends Polygon implements Shape, Drawable, Resizable {
    private double length, breadth;

    Rectangle(double length, double breadth) {
        super(4, "Rectangle");
        this.length = length;
        this.breadth = breadth;
    }

    public double area()      { return length * breadth; }
    public double perimeter() { return 2 * (length + breadth); }
    public String name()      { return "Rectangle"; }
    public void draw()        { System.out.println("  Drawing  [___]  Rectangle " + length + " x " + breadth); }
    public void resize(double f) { length *= f; breadth *= f; }
}

/** Triangle: extends the abstract class + implements two interfaces. */
class Triangle extends Polygon implements Shape, Drawable {
    private final double a, b, c;

    Triangle(double a, double b, double c) {
        super(3, "Triangle");
        if (a + b <= c || a + c <= b || b + c <= a)
            throw new IllegalArgumentException("Sides do not form a triangle");
        this.a = a; this.b = b; this.c = c;
    }

    /** Heron's formula: sqrt(s(s-a)(s-b)(s-c)) where s = half the perimeter */
    public double area() {
        double s = perimeter() / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
    public double perimeter() { return a + b + c; }
    public String name()      { return "Triangle"; }
    public void draw()        { System.out.println("  Drawing  /_\\    Triangle  sides " + a + ", " + b + ", " + c); }
}
