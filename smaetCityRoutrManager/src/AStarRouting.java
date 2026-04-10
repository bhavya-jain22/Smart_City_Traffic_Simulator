import java.util.*;

public class AStarRouting implements RoutingStrategy {

    // Helper class for the Priority Queue
    private static class NodeRecord {
        Intersection node;
        double fCost;
        public NodeRecord(Intersection node, double fCost) {
            this.node = node;
            this.fCost = fCost;
        }
    }

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle) {
        PriorityQueue<NodeRecord> frontier = new PriorityQueue<>(Comparator.comparingDouble(nr -> nr.fCost));
        Map<Intersection, Double> gCosts = new HashMap<>();
        Map<Intersection, Road> cameFrom = new HashMap<>();

        frontier.add(new NodeRecord(start, 0.0));
        gCosts.put(start, 0.0);

        while (!frontier.isEmpty()) {
            Intersection current = frontier.poll().node;

            if (current.equals(target)) break; // Path found!

            for (Road edge : current.getConnectedRoads()) {
                
                // 1. HARD CONSTRAINT
                if (edge.isUnderConstruction()) continue; 

                // 2. SOFT CONSTRAINT & TRAFFIC
                double potholePenalty = edge.getPotholeCount() * 50.0;
                double trafficPenalty = edge.getTrafficDensity() * 1000.0;
                double weatherPenalty = edge.getLength() * HeadLessEngine.weatherMultiplier;

                // VIVA FLEX: The Core A* Math
                double tentativeGCost = gCosts.get(current) + weatherPenalty + potholePenalty + trafficPenalty;

                if (tentativeGCost < gCosts.getOrDefault(edge.getEnd(), Double.MAX_VALUE)) {
                    gCosts.put(edge.getEnd(), tentativeGCost);
                    cameFrom.put(edge.getEnd(), edge);
                    
                    // h(n): Euclidean Distance Heuristic
                    double hCost = Math.sqrt(Math.pow(target.getX() - edge.getEnd().getX(), 2) + 
                                             Math.pow(target.getY() - edge.getEnd().getY(), 2));
                    
                    frontier.add(new NodeRecord(edge.getEnd(), tentativeGCost + hCost));
                }
            }
        }

        // Backtrack to build the final list of roads
        LinkedList<Road> path = new LinkedList<>();
        Intersection curr = target;
        while (cameFrom.containsKey(curr)) {
            Road r = cameFrom.get(curr);
            path.addFirst(r);
            curr = r.getStart(); // <--- THIS FIXES THE INFINITE LOOP!
        }
        
        return path;
    }
}