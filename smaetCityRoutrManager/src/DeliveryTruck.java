import java.util.ArrayList;
import java.util.List;

public class DeliveryTruck extends Vehicle {
    private List<Intersection> remainingDeliveries;
    private Intersection depot;

    public DeliveryTruck(Intersection start, List<Intersection> deliveries, RoutingStrategy brain) {
        super(start, deliveries.isEmpty() ? start : deliveries.get(0), brain, false);
        this.depot = start;
        this.remainingDeliveries = new ArrayList<>(deliveries);
        
        // Find first target using Nearest Neighbor
        if (!remainingDeliveries.isEmpty()) {
            this.targetDestination = getNearestTarget(start, remainingDeliveries);
            this.remainingDeliveries.remove(this.targetDestination);
            this.currentPath = routingBrain.calculateRoute(start, this.targetDestination, this);
        }
    }

    private Intersection getNearestTarget(Intersection current, List<Intersection> targets) {
        Intersection nearest = targets.get(0);
        double minDist = Double.MAX_VALUE;
        for (Intersection target : targets) {
            double dist = Math.sqrt(Math.pow(current.getX() - target.getX(), 2) + Math.pow(current.getY() - target.getY(), 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = target;
            }
        }
        return nearest;
    }

    @Override
    public void moveNextTick() {
        if (currentPath == null || currentPath.isEmpty()) {
            if (!remainingDeliveries.isEmpty()) {
                // TSP Nearest Neighbor: find next closest delivery
                this.targetDestination = getNearestTarget(currentLocation, remainingDeliveries);
                this.remainingDeliveries.remove(this.targetDestination);
                this.currentPath = routingBrain.calculateRoute(currentLocation, targetDestination, this);
            } else if (targetDestination != depot) {
                // Return to depot
                this.targetDestination = depot;
                this.currentPath = routingBrain.calculateRoute(currentLocation, depot, this);
            } else {
                // Finished all deliveries and back to depot
                return;
            }
        }
        super.moveNextTick();
    }
}
