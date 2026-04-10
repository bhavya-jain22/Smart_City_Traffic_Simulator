import java.util.*;

public class AStarRouting implements RoutingStrategy {

    private class NodeRecord implements Comparable<NodeRecord> {
        Intersection node; // node we are working
        double fCost; // Total cost (Actual traffic + Heuristic Cost using Euclidien formula )

        public NodeRecord(Intersection node, double fCost) {  // constructor 
            this.node = node; 
            this.fCost = fCost;
        }

        public int compareTo(NodeRecord other) { // for PQ comparing teo objects 
            return Double.compare(this.fCost, other.fCost);
        }
    }

    //  Straight-line distance between two intersections
    private double getHeuristic(Intersection a, Intersection b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    @Override 
    public List<Road> calculateRoute(Intersection start, Intersection target, Map<Intersection, List<Road>> graph) {
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();  // shrtest path first Node,Cost
        Map<Intersection, Double> gCosts = new HashMap<>(); // Actual cost from start used to compare if we found any new cost 
        Map<Intersection, Road> previousEdge = new HashMap<>(); // store where are we coming from 

        for (Intersection i : graph.keySet())
            gCosts.put(i, Double.MAX_VALUE);
        gCosts.put(start, 0.0);
        pq.add(new NodeRecord(start, getHeuristic(start, target)));

        while (!pq.isEmpty()) {
            Intersection current = pq.poll().node; // top og pq grab it and remove it rerturn node of it from Node Record
            if (current == target)
                break;

            for (Road edge : graph.getOrDefault(current, new ArrayList<>())) { //return balnk if no  neibour or the roads connected safe for handling exception inside java.util.map
                Intersection neighbor = edge.getEndNode(); // return the other end of the road
                double tentativeGCost = gCosts.get(current) + edge.getLength() + (edge.getTrafficDensity() * 10.0);  // total cost for this path crowded is 0.0 to 1.0
                // ex- rajpur 5 traffic 1.0 cost 15km and a road have 7km 0.0 cost 7km 

                if (tentativeGCost < gCosts.get(neighbor)) {
                    gCosts.put(neighbor, tentativeGCost);
                    previousEdge.put(neighbor, edge);

                    double fCost = tentativeGCost + getHeuristic(neighbor, target);
                    pq.add(new NodeRecord(neighbor, fCost));
                }
            }
        }

        List<Road> path = new LinkedList<>(); // building path backward adding in front 
        Intersection curr = target;
        while (previousEdge.containsKey(curr)) {
            Road r = previousEdge.get(curr);
            path.add(0, r);
            curr = r.getStartNode();
        }
        return path;
    }
}