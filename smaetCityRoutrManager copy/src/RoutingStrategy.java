import java.util.List;

public interface RoutingStrategy {
    // Passing the Vehicle object allows the algorithm to check for polymorphism (is it a Taxi?)
    List<Road> calculateRoute(Intersection start, Intersection target, Vehicle requestingVehicle);
}