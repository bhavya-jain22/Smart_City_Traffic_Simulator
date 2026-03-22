import java.util.List;
import java.util.Map;

public class Vehicle {
    private String id;
    private Intersection destination;
    private List<Road> currentRoute;
    private int routeIndex;
    private RoutingStrategy router;

    public Vehicle(String id, Intersection start, Intersection destination, RoutingStrategy router,
            Map<Intersection, List<Road>> graph) {
        this.id = id;
        this.destination = destination;
        this.router = router;
        this.routeIndex = 0;

        System.out.println(
                "🚗 Vehicle " + id + " spawned at " + start.getName() + " heading to " + destination.getName());
        this.currentRoute = router.calculateRoute(start, destination, graph);
        System.out.println("   -> Route planned: " + currentRoute);
    }

    public void moveStep() {
        if (routeIndex >= currentRoute.size()) {
            System.out.println("🏁 Vehicle " + id + " has reached its destination (" + destination.getName() + ")!");
            return;
        }

        Road currentRoad = currentRoute.get(routeIndex);
        Intersection upcomingIntersection = currentRoad.getEndNode();

        if (upcomingIntersection.getLightState().equals("RED") && upcomingIntersection != destination) {
            System.out.println("🛑 Vehicle " + id + " is waiting at a RED light at " + upcomingIntersection.getName());
        } else {
            System.out.println(
                    "💨 Vehicle " + id + " drives along road " + currentRoad.toString() + " past Green light.");
            routeIndex++; // Move to the next road in the list
        }
    }

    public boolean hasArrived() {
        return routeIndex >= currentRoute.size();
    }
}