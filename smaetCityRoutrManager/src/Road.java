import java.util.concurrent.atomic.AtomicInteger;

public class Road {
    private Intersection start, end;
    private double length;
    private int capacity;

    // VIVA FLEX: Thread-safe variable to prevent JVM crashes when 100 cars move at
    // once
    private AtomicInteger currentCars;

    // Hazards
    private boolean isUnderConstruction;
    private int potholeCount;

    public Road(Intersection start, Intersection end, double length, int capacity, boolean isUnderConstruction,
            int potholes) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.capacity = capacity;
        this.currentCars = new AtomicInteger(0);
        this.isUnderConstruction = isUnderConstruction;
        this.potholeCount = potholes;
    }

    // --- SYNCHRONIZED TRAFFIC LOGIC ---
    public synchronized void enterRoad() {
        currentCars.incrementAndGet();
    }

    public synchronized void exitRoad() {
        currentCars.decrementAndGet();
    }

    public double getTrafficDensity() {
        return (double) currentCars.get() / capacity;
    }

    // Getters
    public Intersection getEnd() {
        return end;
    }

    public Intersection getStart() {
        return start; }
    public double getLength() {
        return length;
    }

    public boolean isUnderConstruction() {
        return isUnderConstruction;
    }

    public int getPotholeCount() {
        return potholeCount;
    }
}