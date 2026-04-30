import java.util.*;

/**
 * Bellman-Ford Routing Algorithm (No DP / No memoization table)
 * Performs edge relaxation V-1 times on all edges.
 * Handles negative weight edges and detects unreachable nodes.
 */
public class BellmanFordRouting implements RoutingStrategy {

    @Override
    public List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle) {
        // Collect ALL edges in the graph by BFS from start
        Set<Intersection> allNodes = new HashSet<>();
        List<Road> allEdges = new ArrayList<>();

        Queue<Intersection> bfsQueue = new LinkedList<>();
        bfsQueue.add(start);
        allNodes.add(start);

        while (!bfsQueue.isEmpty()) {
            Intersection curr = bfsQueue.poll();
            for (Road road : curr.getConnectedRoads()) {
                if (road.isUnderConstruction()) continue;
                allEdges.add(road);
                if (!allNodes.contains(road.getEnd())) {
                    allNodes.add(road.getEnd());
                    bfsQueue.add(road.getEnd());
                }
            }
        }

        // Bellman-Ford: Initialize distances
        Map<Intersection, Double> dist = new HashMap<>();
        Map<Intersection, Road> prev = new HashMap<>();

        for (Intersection node : allNodes) {
            dist.put(node, Double.MAX_VALUE);
        }
        dist.put(start, 0.0);

        int V = allNodes.size();

        // Relax all edges V-1 times
        for (int i = 0; i < V - 1; i++) {
            boolean updated = false;
            for (Road edge : allEdges) {
                Intersection u = edge.getStart();
                Intersection v = edge.getEnd();

                if (dist.get(u) == Double.MAX_VALUE) continue;

                // Dynamic traffic penalty (same approach as A*)
                double trafficPenalty = 1.0 + (edge.getTrafficDensity() * 4.0);
                double newDist = dist.get(u) + (edge.getLength() * trafficPenalty);

                if (newDist < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, edge);
                    updated = true;
                }
            }
            // Early termination if no update happened
            if (!updated) break;
        }

        // Reconstruct path from target back to start
        LinkedList<Road> path = new LinkedList<>();
        Intersection curr = target;

        while (prev.containsKey(curr)) {
            Road r = prev.get(curr);
            path.addFirst(r);
            curr = r.getStart();
        }

        return path;
    }
}
