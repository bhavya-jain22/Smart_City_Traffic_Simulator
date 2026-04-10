import java.util.List;

public class Vehicle {
    protected Intersection currentLocation;
    protected Intersection targetDestination;
    protected List<Road> currentPath;
    protected RoutingStrategy routingBrain;

    public Vehicle(Intersection start, Intersection target, RoutingStrategy brain) {
        this.currentLocation = start;
        this.targetDestination = target;
        this.routingBrain = brain;
        this.currentPath = routingBrain.calculateRoute(start, target, this);
    }

    public void moveNextTick() {
        if (currentPath == null || currentPath.isEmpty())
            return; // Reached destination

        Road nextRoad = currentPath.get(0);

        //If road is 70% full, abandon path and recalculate.
        if (nextRoad.getTrafficDensity() >= 0.7) {
            System.out.println("Traffic Jam Avoided by Vehicle! Recalculating...");
            this.currentPath = routingBrain.calculateRoute(currentLocation, targetDestination, this);
            if (currentPath.isEmpty())
                return;
            nextRoad = currentPath.get(0);
        }

        // Simulate moving across the road
        nextRoad.enterRoad();
        this.currentLocation = nextRoad.getEnd();
        nextRoad.exitRoad();

        currentPath.remove(0); // Pop finished road
    }

    public Intersection getCurrentLocation() {
        return currentLocation;
    }
    
    public boolean isFinished() {
        return currentPath == null || currentPath.isEmpty();
    }
}

class Taxi extends Vehicle {
    private double distanceTraveledMeters = 0.0;
    private final double RATE_PER_KM = 15.0; 

    public Taxi(Intersection start, Intersection target, RoutingStrategy brain) {
        super(start, target, brain);
    }

    @Override
    public void moveNextTick() {
        if (currentPath == null || currentPath.isEmpty()) {
            if (distanceTraveledMeters > 0) {
                double distanceInKm = distanceTraveledMeters / 1000.0;

                // Calculate realistic fare
                double fare = distanceInKm * RATE_PER_KM;

                System.out.println(String.format("TAXI ARRIVED! Distance: %.2f km | Fare Collected: Rs. %.2f",
                        distanceInKm, fare));

                distanceTraveledMeters = 0;
            }
            return;
        }

        // Track distance before moving
        distanceTraveledMeters += currentPath.get(0).getLength();
        super.moveNextTick();
    }
}