import java.util.*;

public class AStarRouting implements RoutingStrategy {

    private class NodeRecord implements Comparable<NodeRecord> {
        Intersection node;
        double fCost; // Total cost (Actual traffic + Heuristic guess)

        public NodeRecord(Intersection node, double fCost) {
            this.node = node;
            this.fCost = fCost;
        }

        public int compareTo(NodeRecord other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }

    // The Brain of A*: Straight-line distance between two intersections
    private double getHeuristic(Intersection a, Intersection b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Map<Intersection, List<Road>> graph) {
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();
        Map<Intersection, Double> gCosts = new HashMap<>(); // Actual cost from start
        Map<Intersection, Road> previousEdge = new HashMap<>();

        for (Intersection i : graph.keySet())
            gCosts.put(i, Double.MAX_VALUE);
        gCosts.put(start, 0.0);
        pq.add(new NodeRecord(start, getHeuristic(start, target)));

        while (!pq.isEmpty()) {
            Intersection current = pq.poll().node;
            if (current == target)
                break;

            for (Road edge : graph.getOrDefault(current, new ArrayList<>())) {
                Intersection neighbor = edge.getEndNode();
                double tentativeGCost = gCosts.get(current) + edge.getLength() + (edge.getTrafficDensity() * 10.0);

                if (tentativeGCost < gCosts.get(neighbor)) {
                    gCosts.put(neighbor, tentativeGCost);
                    previousEdge.put(neighbor, edge);

                    double fCost = tentativeGCost + getHeuristic(neighbor, target);
                    pq.add(new NodeRecord(neighbor, fCost));
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