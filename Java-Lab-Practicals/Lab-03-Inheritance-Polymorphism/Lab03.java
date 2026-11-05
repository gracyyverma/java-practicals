/*
 * ============================================================================
 *  LAB 03 - Multilevel Inheritance & Polymorphism (Vehicle -> Car -> ElectricCar)
 * ============================================================================
 *  Aim : Design a class hierarchy Vehicle -> Car -> ElectricCar. Implement
 *        multilevel inheritance with attributes and methods. Demonstrate
 *        method overriding at each level, use of the super keyword and
 *        dynamic method dispatch. Add a final method in the base class and
 *        verify its behaviour.
 *
 *  CO2 : Apply inheritance and polymorphism in Java programs
 *  Run : java Lab03.java
 * ============================================================================
 */
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class Lab03 {

    public static void main(String[] args) throws Exception {
        line('=');
        System.out.println("  LAB 03 : VEHICLE -> CAR -> ELECTRIC CAR  (MULTILEVEL INHERITANCE)");
        line('=');

        // ---- 1. Constructor chaining through super(...) ----
        section("1. Constructor chaining with super(...)");
        System.out.println("  new ElectricCar(\"Tata\", \"Nexon EV\", 2024, 4, 40.5, 465)");
        Vehicle.traceConstructors = true;
        ElectricCar ev = new ElectricCar("Tata", "Nexon EV", 2024, 4, 40.5, 465);
        Vehicle.traceConstructors = false;
        System.out.println("  => Parent constructors always run first (top of the hierarchy down).");

        Vehicle bike = new Vehicle("Hero", "Splendor", 2020);
        Car car = new Car("Maruti", "Swift", 2022, 4);

        // ---- 2. Method overriding at each level + super.method() ----
        section("2. Method overriding at every level (+ super.start())");
        System.out.println("  bike.start()  [Vehicle]");
        bike.start();
        System.out.println("  car.start()   [Car overrides, calls super.start()]");
        car.start();
        System.out.println("  ev.start()    [ElectricCar overrides, calls super.start()]");
        ev.start();

        System.out.println();
        System.out.println("  describe() builds on super.describe() at each level:");
        System.out.println("    Vehicle     : " + bike.describe());
        System.out.println("    Car         : " + car.describe());
        System.out.println("    ElectricCar : " + ev.describe());

        // ---- 3. Dynamic method dispatch ----
        section("3. Dynamic method dispatch (parent reference, child object)");
        Vehicle[] garage = { bike, car, ev };          // all stored as Vehicle references
        System.out.printf("  %-17s | %-15s | %s%n", "Reference type", "Runtime object", "ref.fuelType()  -> decided at RUNTIME");
        System.out.println("  " + "-".repeat(18) + "+" + "-".repeat(17) + "+" + "-".repeat(38));
        for (Vehicle ref : garage) {
            System.out.printf("  %-17s | %-15s | %s%n", "Vehicle", ref.getClass().getSimpleName(), ref.fuelType());
        }

        // ---- 4. final method in the base class ----
        section("4. final method in the base class - verify its behaviour");
        for (Vehicle ref : garage) {
            System.out.printf("  %-12s.registrationInfo() -> %s%n", ref.getClass().getSimpleName(), ref.registrationInfo());
        }
        Method m = Vehicle.class.getDeclaredMethod("registrationInfo");
        System.out.println("  Is Vehicle.registrationInfo() final?     " + Modifier.isFinal(m.getModifiers()));
        System.out.println("  Does Car declare its own version?        " + declares(Car.class, "registrationInfo"));
        System.out.println("  Does ElectricCar declare its own version? " + declares(ElectricCar.class, "registrationInfo"));

        System.out.println();
        System.out.println("  Proof: asking the Java compiler to compile a subclass that overrides it...");
        String badCode =
                "class Vehicle { final String registrationInfo() { return \"base\"; } }\n" +
                "class BadCar extends Vehicle { String registrationInfo() { return \"hacked\"; } }";
        System.out.println("    Compiler says: " + compileAndGetError(badCode));

        line('=');
        System.out.println("  Lab 03 completed successfully");
        line('=');
    }

    /** true if the class itself declares (not inherits) the method. */
    static boolean declares(Class<?> cls, String name) {
        try { cls.getDeclaredMethod(name); return true; }
        catch (NoSuchMethodException e) { return false; }
    }

    /** Compiles a source string in memory and returns the first error message. */
    static String compileAndGetError(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///BadCar.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignore) { return source; }
        };
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean ok = compiler.getTask(null, null, diagnostics,
                List.of("-d", System.getProperty("java.io.tmpdir")), null, List.of(file)).call();
        if (ok) return "compiled fine (unexpected!)";
        Diagnostic<?> d = diagnostics.getDiagnostics().get(0);
        return "compile ERROR\n      " + d.getMessage(null).replaceAll("\\n\\s*", "\n      ");
    }

    static void section(String t) { System.out.println("\n>> " + t); line('-'); }
    static void line(char c) { System.out.println(String.valueOf(c).repeat(78)); }
}

// ============================================================================
//  LEVEL 1 : Vehicle (base class)
// ============================================================================
class Vehicle {
    static boolean traceConstructors = false;     // only used to show the chaining order

    protected String brand;
    protected String model;
    protected int year;

    Vehicle(String brand, String model, int year) {
        this.brand = brand;
        this.model = model;
        this.year = year;
        if (traceConstructors) System.out.println("    (1) Vehicle constructor      -> brand, model, year set");
    }

    void start() {
        System.out.println("      Vehicle.start()      : key turned, engine ON");
    }

    String describe() {
        return year + " " + brand + " " + model;
    }

    String fuelType() {
        return "Petrol (Vehicle version)";
    }

    /** final -> NO subclass can override this. Every vehicle uses these rules. */
    final String registrationInfo() {
        return "REG/" + brand.substring(0, 3).toUpperCase() + "/" + year + "  (same rule for all)";
    }
}

// ============================================================================
//  LEVEL 2 : Car extends Vehicle
// ============================================================================
class Car extends Vehicle {
    protected int doors;

    Car(String brand, String model, int year, int doors) {
        super(brand, model, year);                 // must be the first statement
        this.doors = doors;
        if (traceConstructors) System.out.println("    (2) Car constructor          -> doors set");
    }

    @Override
    void start() {
        super.start();                             // reuse parent behaviour
        System.out.println("      Car.start()          : seat-belt check, gear in PARK");
    }

    @Override
    String describe() {
        return super.describe() + ", " + doors + " doors";
    }

    @Override
    String fuelType() {
        return "Petrol/Diesel (Car version)";
    }
}

// ============================================================================
//  LEVEL 3 : ElectricCar extends Car
// ============================================================================
class ElectricCar extends Car {
    private final double batteryKWh;
    private final int rangeKm;

    ElectricCar(String brand, String model, int year, int doors, double batteryKWh, int rangeKm) {
        super(brand, model, year, doors);
        this.batteryKWh = batteryKWh;
        this.rangeKm = rangeKm;
        if (traceConstructors) System.out.println("    (3) ElectricCar constructor  -> battery, range set");
    }

    @Override
    void start() {
        super.start();                             // runs Car.start(), which runs Vehicle.start()
        System.out.println("      ElectricCar.start()  : battery 82%, silent motor READY");
    }

    @Override
    String describe() {
        return super.describe() + ", " + batteryKWh + " kWh, " + rangeKm + " km range";
    }

    @Override
    String fuelType() {
        return "Electric " + batteryKWh + " kWh (ElectricCar version)";
    }
}
