import java.util.List;
import java.util.Map;

public interface RoutingStrategy {
    List<Road> calculateRoute(Intersection start, Intersection destination, Map<Intersection, List<Road>> cityGraph);
}
