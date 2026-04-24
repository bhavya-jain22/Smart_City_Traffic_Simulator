import java.util.*;

public class DijkstraRouting implements RoutingStrategy {

    private static class NodeRecord {
        Intersection node;
        double gCost; // Total cost to reach this node
        public NodeRecord(Intersection node, double gCost) {
            this.node = node;
            this.gCost = gCost;
        }
    }

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle) {
        PriorityQueue<NodeRecord> frontier = new PriorityQueue<>(Comparator.comparingDouble(nr -> nr.gCost));
        Map<Intersection, Double> gCosts = new HashMap<>();
        Map<Intersection, Road> cameFrom = new HashMap<>();

        frontier.add(new NodeRecord(start, 0.0));
        gCosts.put(start, 0.0);

        while (!frontier.isEmpty()) {
            Intersection current = frontier.poll().node;

            if (current.equals(target)) break;

            for (Road edge : current.getConnectedRoads()) {
                if (edge.isUnderConstruction()) continue; 

                double potholePenalty = edge.getPotholeCount() * 50.0;
                double trafficPenalty = edge.getTrafficDensity() * 1000.0;
                double weatherPenalty = edge.getLength() * HeadLessEngine.weatherMultiplier;

                // Dijkstra: Only consider path cost (no heuristic)
                double tentativeGCost = gCosts.get(current) + weatherPenalty + potholePenalty + trafficPenalty;

                if (tentativeGCost < gCosts.getOrDefault(edge.getEnd(), Double.MAX_VALUE)) {
                    gCosts.put(edge.getEnd(), tentativeGCost);
                    cameFrom.put(edge.getEnd(), edge);
                    
                    frontier.add(new NodeRecord(edge.getEnd(), tentativeGCost));
                }
            }
        }

        LinkedList<Road> path = new LinkedList<>();
        Intersection curr = target;
        while (cameFrom.containsKey(curr)) {
            Road r = cameFrom.get(curr);
            path.addFirst(r);
            curr = r.getStart();
        }
        
        return path;
    }
}
