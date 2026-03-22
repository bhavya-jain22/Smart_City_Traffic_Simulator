import java.util.*;

public class HeadlessEngine {
    public static void main(String[] args) {
        System.out.println("=== STARTING SMART CITY BACKEND ENGINE ===\n");

        // 1. Build the Graph Nodes (Intersections)
        Intersection nodeA = new Intersection("A");
        Intersection nodeB = new Intersection("B");
        Intersection nodeC = new Intersection("C");

        // 2. Build the Graph Edges (Roads)
        Road roadAB = new Road(nodeA, nodeB, 5.0);
        Road roadBC = new Road(nodeB, nodeC, 5.0);
        Road roadAC = new Road(nodeA, nodeC, 15.0); // A longer direct route

        // 3. Setup the Adjacency List (City Map)
        Map<Intersection, List<Road>> cityGraph = new HashMap<>();
        cityGraph.put(nodeA, Arrays.asList(roadAB, roadAC));
        cityGraph.put(nodeB, Arrays.asList(roadBC));
        cityGraph.put(nodeC, new ArrayList<>());

        // 4. Spawn a Vehicle using Dijkstra Strategy
        RoutingStrategy dijkstra = new DijkstraRouting();
        Vehicle car1 = new Vehicle("V-001", nodeA, nodeC, dijkstra, cityGraph);

        System.out.println("\n--- SIMULATION STARTED ---");

        // 5. The Engine Loop (10 ticks)
        for (int tick = 1; tick <= 10; tick++) {
            System.out.println("\n[Tick " + tick + "]");

            // Update environment
            nodeA.updateLight();
            nodeB.updateLight();
            nodeC.updateLight();

            // Move cars
            if (!car1.hasArrived()) {
                car1.moveStep();
            } else {
                break; // Stop simulation if car arrived
            }

            // Slow down the console output so you can read it
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        System.out.println("\n=== SIMULATION ENDED ===");
    }
}