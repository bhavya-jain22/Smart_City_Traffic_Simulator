import java.util.concurrent.atomic.AtomicInteger;

public class Road {
    private Intersection start, end;
    private double length;
    private int capacity;

    // variable to prevent crash when 100 cars move at once
    private AtomicInteger currentCars;
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

    public synchronized void enterRoad() {
        currentCars.incrementAndGet();
    }

    public synchronized void exitRoad() {
        currentCars.decrementAndGet();
    }

    public double getTrafficDensity() {
        return (double) currentCars.get() / capacity;
    }

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

    public void setUnderConstruction(boolean underConstruction) {
        this.isUnderConstruction = underConstruction;
    }

    public int getCapacity() { return capacity; }

    /** Temporarily reduce capacity to force congestion in demos */
    public void setCapacity(int cap) {
        this.capacity = Math.max(1, cap);
    }

    public int getPotholeCount() {
        return potholeCount;
    }
}