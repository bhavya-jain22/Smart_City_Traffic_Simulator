import java.util.*;

public class BellmanFordRouting implements RoutingStrategy {

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Map<Intersection, List<Road>> graph) {
        Map<Intersection, Double> distances = new HashMap<>();
        Map<Intersection, Road> previousEdge = new HashMap<>();

        for (Intersection i : graph.keySet()) distances.put(i, Double.MAX_VALUE);
        distances.put(start, 0.0);

        int totalNodes = graph.size();

        // Relax all edges V-1 times
        for (int i = 1; i < totalNodes; i++) {
            for (Intersection u : graph.keySet()) {
                for (Road edge : graph.getOrDefault(u, new ArrayList<>())) {
                    Intersection neighbor = edge.getEndNode();
                    double weight = edge.getLength() + (edge.getTrafficDensity() * 10.0);

                    if (distances.get(u) != Double.MAX_VALUE && distances.get(u) + weight < distances.get(neighbor)) {
                        distances.put(neighbor, distances.get(u) + weight);
                        previousEdge.put(neighbor, edge);
                    }
                }
            }
        }

        List<Road> path = new ArrayList<>();
        Intersection curr = target;
        while (previousEdge.containsKey(curr)) {
            Road r = previousEdge.get(curr);
            path.add(0, r);
            curr = r.getStartNode();
        }
        return path;
    }
}