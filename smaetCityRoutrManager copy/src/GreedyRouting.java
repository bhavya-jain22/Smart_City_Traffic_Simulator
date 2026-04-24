import java.util.*;

public class GreedyRouting implements RoutingStrategy {

    private static class NodeRecord {
        Intersection node;
        double hCost; // Heuristic cost (distance to target)
        public NodeRecord(Intersection node, double hCost) {
            this.node = node;
            this.hCost = hCost;
        }
    }

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle) {
        PriorityQueue<NodeRecord> frontier = new PriorityQueue<>(Comparator.comparingDouble(nr -> nr.hCost));
        Set<Intersection> visited = new HashSet<>();
        Map<Intersection, Road> cameFrom = new HashMap<>();

        // Initial distance is just from start to target
        double startHCost = Math.sqrt(Math.pow(target.getX() - start.getX(), 2) + Math.pow(target.getY() - start.getY(), 2));
        frontier.add(new NodeRecord(start, startHCost));
        
        while (!frontier.isEmpty()) {
            Intersection current = frontier.poll().node;

            if (current.equals(target)) break; 
            
            if (visited.contains(current)) continue;
            visited.add(current);

            for (Road edge : current.getConnectedRoads()) {
                if (edge.isUnderConstruction()) continue; 
                if (visited.contains(edge.getEnd())) continue;

                // Greedy ignores gCost and only cares about getting closer physically
                double hCost = Math.sqrt(Math.pow(target.getX() - edge.getEnd().getX(), 2) + 
                                         Math.pow(target.getY() - edge.getEnd().getY(), 2));
                
                // Add minor traffic awareness so it doesn't get completely stuck
                double trafficPenalty = edge.getTrafficDensity() * 5.0; 

                if (!cameFrom.containsKey(edge.getEnd())) {
                    cameFrom.put(edge.getEnd(), edge);
                    frontier.add(new NodeRecord(edge.getEnd(), hCost + trafficPenalty));
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
