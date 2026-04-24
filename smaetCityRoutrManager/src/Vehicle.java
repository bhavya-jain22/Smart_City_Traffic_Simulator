import java.util.List;

public class Vehicle {
    protected Intersection currentLocation;
    protected Intersection targetDestination;
    protected List<Road> currentPath;
    protected RoutingStrategy routingBrain;

    // Added fields for UI Animation
    public double drawX;
    public double drawY;
    protected double roadProgress = 0.0;
    
    // Benchmark tracking
    public int ticksAlive = 0;
    public double totalDistanceTraveled = 0.0;

    // Visual State Tracking
    public boolean hasRerouted = false;
    public boolean isWaitingOrSlow = false;
    public boolean isAmbulance = false;

    public Vehicle(Intersection start, Intersection target, RoutingStrategy routingBrain, boolean isAmbulance) {
        this.currentLocation = start;
        this.targetDestination = target;
        this.routingBrain = routingBrain;
        this.isAmbulance = isAmbulance;
        this.currentPath = routingBrain.calculateRoute(start, target, this);
        this.drawX = start.getX();
        this.drawY = start.getY();
    }

    public void moveNextTick() {
        if (isFinished() || currentPath == null || currentPath.isEmpty()) {
            return;
        }

        Road nextRoad = currentPath.get(0);

        //If road is 70% full or suddenly blocked by user, abandon path and recalculate. We only do this at an intersection.
        if (roadProgress == 0.0 && (nextRoad.getTrafficDensity() >= 0.7 || nextRoad.isUnderConstruction())) {
            this.currentPath = routingBrain.calculateRoute(currentLocation, targetDestination, this);
            if (currentPath == null || currentPath.isEmpty()) {
                isWaitingOrSlow = true;
                ticksAlive++;
                return; // No valid path, wait here
            }
            nextRoad = currentPath.get(0);
            hasRerouted = true; // Mark as having performed a smart detour
        }

        // Enter the road
        if (roadProgress == 0.0) {
            // Check traffic light before entering
            boolean roadIsHorizontal = Math.abs(nextRoad.getEnd().getX() - nextRoad.getStart().getX()) > 
                                       Math.abs(nextRoad.getEnd().getY() - nextRoad.getStart().getY());
            
            // If the road is horizontal but the light is vertical (or vice versa), we must wait
            // AMBULANCES IGNORE RED LIGHTS
            if (!isAmbulance && roadIsHorizontal != currentLocation.isHorizontalGreen) {
                // Waiting at red light
                isWaitingOrSlow = true;
                ticksAlive++;
                return;
            }

            nextRoad.enterRoad();
        }

        // Check if currently on a crowded road (Ambulances push through traffic)
        if (!isAmbulance && nextRoad.getTrafficDensity() > 0.5) {
            isWaitingOrSlow = true;
        } else {
            isWaitingOrSlow = false;
        }

        // Benchmark update
        ticksAlive++;

        // Calculate visual progress based on a fixed step size (e.g. 15.0 units per tick)
        // You can adjust '15.0' to make vehicles move faster or slower visually
        // Move along the road (Ambulances are 2x faster, and weather affects global speed)
        double speed = (isAmbulance ? 30.0 : 15.0) * SmartCityGUI.getGlobalSpeedMultiplier();
        double progressIncrement = speed / nextRoad.getLength(); 
        
        if (roadProgress + progressIncrement > 1.0) {
            progressIncrement = 1.0 - roadProgress;
        }
        totalDistanceTraveled += (progressIncrement * nextRoad.getLength());
        
        roadProgress += progressIncrement;

        if (roadProgress >= 1.0) {
            // Reached the end of the road
            this.currentLocation = nextRoad.getEnd();
            this.drawX = currentLocation.getX();
            this.drawY = currentLocation.getY();
            nextRoad.exitRoad();
            currentPath.remove(0); // Pop finished road
            roadProgress = 0.0;
        } else {
            // Interpolate position for drawing
            double startX = nextRoad.getStart().getX();
            double startY = nextRoad.getStart().getY();
            double endX = nextRoad.getEnd().getX();
            double endY = nextRoad.getEnd().getY();
            this.drawX = startX + (endX - startX) * roadProgress;
            this.drawY = startY + (endY - startY) * roadProgress;
        }
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
        super(start, target, brain, false);
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

        // Track distance incrementally before moving
        Road nextRoad = currentPath.get(0);
        double progressIncrement = 15.0 / nextRoad.getLength();
        if (roadProgress + progressIncrement > 1.0) {
            progressIncrement = 1.0 - roadProgress;
        }
        distanceTraveledMeters += (progressIncrement * nextRoad.getLength());

        super.moveNextTick();
    }
}