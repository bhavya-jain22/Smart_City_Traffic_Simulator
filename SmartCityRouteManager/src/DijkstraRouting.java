import java.util.*;

public class DijkstraRouting implements RoutingStrategy {

    private class NodeRecord implements Comparable<NodeRecord> {
        Intersection node;
        double cost;

        public NodeRecord(Intersection node, double cost) {
            this.node = node;
            this.cost = cost;
        }

        public int compareTo(NodeRecord other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Map<Intersection, List<Road>> graph) {
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();
        Map<Intersection, Double> distances = new HashMap<>();
        Map<Intersection, Road> previousEdge = new HashMap<>();

        for (Intersection i : graph.keySet())
            distances.put(i, Double.MAX_VALUE);
        distances.put(start, 0.0);
        pq.add(new NodeRecord(start, 0.0));

        while (!pq.isEmpty()) {
            Intersection current = pq.poll().node;
            if (current == target)
                break;

            for (Road edge : graph.getOrDefault(current, new ArrayList<>())) {
                Intersection neighbor = edge.getEndNode();
                // Dynamic Weight Formula
                double edgeWeight = edge.getLength() + (edge.getTrafficDensity() * 10.0);
                double newDist = distances.get(current) + edgeWeight;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previousEdge.put(neighbor, edge);
                    pq.add(new NodeRecord(neighbor, newDist));
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