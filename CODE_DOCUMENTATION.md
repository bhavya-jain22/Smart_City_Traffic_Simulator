# Smart City Traffic Simulator - Codebase Documentation

This document serves as a technical manual for the Java codebase. It explains the purpose of each major `.java` file, its core methods, and provides a line-by-line analysis of the most critical logic.

---

## 1. Graph Theory Data Structures

### `Intersection.java` (The Node)
**Purpose:** Represents a single point or junction on the city map. It acts as a "Node" in our Graph Theory model.
**Key Variables:**
* `int id`, `String name`: Unique identifiers for the junction.
* `int x, y`: Coordinate positions for drawing the node on the UI.
* `List<Road> connectedRoads`: An adjacency list storing all roads (edges) leading *out* of this intersection.
* `boolean isHorizontalGreen`: A boolean state for the traffic light system.

**Key Method: `addRoad(Road road)`**
```java
public void addRoad(Road road) {
    connectedRoads.add(road);
}
```
*Explanation:* Appends a new edge to the adjacency list, connecting this node to another.

### `Road.java` (The Edge)
**Purpose:** Represents a directed road connecting two Intersections. It holds the "weights" and penalties for the routing algorithms.
**Key Variables:**
* `Intersection start, end`: The two nodes this edge connects.
* `double length`: The physical distance (base weight).
* `int currentVehicles`: Used to dynamically calculate traffic jams.
* `boolean isUnderConstruction`: A toggleable boolean that acts as an infinite penalty block.

**Key Method: `getTrafficDensity()`**
```java
public double getTrafficDensity() {
    double capacity = length / 15.0; // Assume 1 car takes up 15 units of space
    return Math.min(1.0, currentVehicles / capacity);
}
```
*Explanation:* Calculates congestion as a percentage (0.0 to 1.0). If the density is high, algorithms like A* will add a heavy penalty to this edge.

---

## 2. Pathfinding Algorithms (Strategy Pattern)

To allow the system to swap algorithms easily, we use an interface:
```java
public interface RoutingStrategy {
    List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle);
}
```

### `AStarRouting.java` (The Intelligent Brain)
**Purpose:** Implements the A* search algorithm. It calculates the fastest route by balancing physical distance with live traffic penalties.

**Line-by-Line Breakdown of the Core Logic:**
```java
// 1. Priority Queue to explore the node with the lowest F-cost first.
PriorityQueue<NodeRecord> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.estimatedTotalCost));

// 2. HashMap to keep track of the best-known distance to any intersection.
Map<Intersection, Double> costSoFar = new HashMap<>();

// 3. Initialize the starting node. G-cost is 0. H-cost is straight-line to target.
openList.add(new NodeRecord(start, null, null, 0, heuristic(start, target)));
costSoFar.put(start, 0.0);

while (!openList.isEmpty()) {
    // 4. Pop the most promising node off the queue
    NodeRecord current = openList.poll();

    // 5. If we reached the target, reconstruct the path backwards using the 'fromRoad' pointers
    if (current.node == target) return buildPath(current);

    // 6. Loop through all connected roads (neighboring edges)
    for (Road road : current.node.getConnectedRoads()) {
        
        // 7. If the road is clicked by the user (Construction), treat it as a dead end!
        if (road.isUnderConstruction()) continue;

        // 8. Calculate dynamic penalty. 1.0 is empty, up to 5.0 for heavy traffic!
        double trafficPenalty = 1.0 + (road.getTrafficDensity() * 4.0);
        
        // 9. Calculate new G-Cost (Distance traveled so far + length * penalty)
        double newCost = costSoFar.get(current.node) + (road.getLength() * trafficPenalty);

        // 10. If we found a faster way to this neighbor, update it and add to Queue
        if (!costSoFar.containsKey(road.getEnd()) || newCost < costSoFar.get(road.getEnd())) {
            costSoFar.put(road.getEnd(), newCost);
            double priority = newCost + heuristic(road.getEnd(), target); // F = G + H
            openList.add(new NodeRecord(road.getEnd(), current, road, newCost, priority));
        }
    }
}
```

### `DijkstraRouting.java`
**Purpose:** Similar to A*, but the `heuristic` is always 0. It exhaustively searches in all directions. It guarantees the best path but is computationally slower.

### `GreedyRouting.java`
**Purpose:** Only cares about the `heuristic` (straight-line distance to target). It completely ignores `trafficPenalty` and `costSoFar`. It is very fast but often drives straight into blocked roads.

---

## 3. Simulation Entities

### `Vehicle.java`
**Purpose:** Represents a car/ambulance on the map. It holds its own state, coordinates, and asks its assigned `RoutingStrategy` where to go.

**Line-by-Line Breakdown of `moveNextTick()`:**
```java
public void moveNextTick() {
    // 1. Get the current road the car is driving on
    Road nextRoad = currentPath.get(0);

    // 2. SMART DETOUR LOGIC: If we are at an intersection, check if the road ahead just got blocked or heavily congested (>70%)
    if (roadProgress == 0.0 && (nextRoad.getTrafficDensity() >= 0.7 || nextRoad.isUnderConstruction())) {
        // 3. Ask the algorithm to calculate a brand new route from here to the destination!
        this.currentPath = routingBrain.calculateRoute(currentLocation, targetDestination, this);
        if (currentPath == null || currentPath.isEmpty()) return; // Trapped!
        nextRoad = currentPath.get(0);
        hasRerouted = true; // Changes car color to Magenta!
    }

    // 4. TRAFFIC LIGHT LOGIC
    if (roadProgress == 0.0) {
        boolean roadIsHorizontal = /* logic to check road angle */;
        
        // 5. Normal cars MUST stop if the road direction doesn't match the Green Light. Ambulances ignore this!
        if (!isAmbulance && roadIsHorizontal != currentLocation.isHorizontalGreen) {
            isWaitingOrSlow = true; // Changes car color to Red!
            return; // Exit method, car does not move this tick
        }
        nextRoad.enterRoad(); // Adds +1 to road density
    }

    // 6. SPEED LOGIC: Ambulances are 2x faster. Multiply by Global Weather Speed.
    double speed = (isAmbulance ? 30.0 : 15.0) * SmartCityGUI.getGlobalSpeedMultiplier();
    double progressIncrement = speed / nextRoad.getLength(); 
    
    // 7. Move the car forward mathematically
    roadProgress += progressIncrement;

    // 8. If we reached the end of the road, pop it from the path list
    if (roadProgress >= 1.0) {
        this.currentLocation = nextRoad.getEnd();
        nextRoad.exitRoad(); // Removes -1 from road density
        currentPath.remove(0); 
        roadProgress = 0.0;
    }
}
```

---

## 4. UI and Benchmarking Engine

### `SmartCityGUI.java`
**Purpose:** The main dashboard containing the JFrame, buttons, and the Master Timer Loop.
**Key Mechanic:** Uses a `javax.swing.Timer` running at 33ms (approx 30 FPS). Every tick, it calls `moveNextTick()` on all vehicles and then calls `simulationPanel.repaint()` to redraw the graphics.

### `SimulationPanel.java`
**Purpose:** Handles all `Graphics2D` drawing.
**Key Mechanic:** Iterates through `cityGraph` to draw lines (roads) and dots (intersections). It changes colors based on properties (e.g., drawing Orange if `road.isUnderConstruction()`).

### `RaceModeGUI.java`
**Purpose:** A specialized UI that instantiates three completely separate `SimulationPanel`s. It utilizes identical seeded randomness to spawn the exact same vehicles across all three maps simultaneously.

### `CSVExporter.java`
**Purpose:** A utility to handle File I/O.
**Key Mechanic:** 
```java
FileWriter fw = new FileWriter("benchmark_results.csv", true);
PrintWriter pw = new PrintWriter(fw);
pw.printf("%s,%s,%s,%b,%d... \n", algorithm, start, target, isAmbulance, ticks);
```
*Explanation:* Uses `FileWriter` in append mode (`true`) to write comma-separated strings to a file, allowing the data to be opened in Excel for analysis.
