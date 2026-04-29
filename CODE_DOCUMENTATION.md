# Smart City Traffic Simulator — Codebase Documentation

This document is the technical manual for the Java codebase. It explains every major `.java` file, its core methods, recent changes, and provides annotated code walkthroughs of the most critical logic.

> **Last Updated:** April 2026 — reflects all features added in the final sprint (Race Mode, Step Visualizer, Force Bottleneck, Reroute Highlight, Speed Multiplier, and Post-Race Analysis).

---

## 1. Graph Theory Data Structures

### `Intersection.java` — The Graph Node
**Purpose:** Represents a single junction on the city map (a vertex in graph theory terms).

| Field | Type | Purpose |
|---|---|---|
| `id`, `name` | `int`, `String` | Unique identifiers |
| `x`, `y` | `int` | Screen-coordinate position for rendering |
| `connectedRoads` | `List<Road>` | Adjacency list — all outgoing edges |
| `isHorizontalGreen` | `boolean` | Current traffic light state |

**Key Method: `addRoad(Road road)`**
```java
public void addRoad(Road road) {
    connectedRoads.add(road); // Appends a directed edge to this node's adjacency list
}
```

---

### `Road.java` — The Directed Edge
**Purpose:** A weighted, directed edge connecting two Intersections. Holds all real-time traffic state.

| Field | Type | Purpose |
|---|---|---|
| `start`, `end` | `Intersection` | The two nodes this edge connects |
| `length` | `double` | Base weight for routing algorithms |
| `capacity` | `int` | Max vehicles before the road is considered jammed |
| `currentCars` | `AtomicInteger` | Thread-safe live vehicle count |
| `isUnderConstruction` | `boolean` | When `true`, algorithms treat this edge as impassable |
| `potholeCount` | `int` | Adds a speed penalty modifier |

**Key Method: `getTrafficDensity()`**
```java
public double getTrafficDensity() {
    return (double) currentCars.get() / capacity;
    // Returns 0.0 (empty) to 1.0+ (jammed)
    // capacity is randomly assigned 5–19 at map generation time
}
```

**New Method: `setCapacity(int cap)`** *(added in final sprint)*
```java
public void setCapacity(int cap) {
    this.capacity = Math.max(1, cap);
    // Used by Force Bottleneck Demo to artificially reduce capacity to 1,
    // guaranteeing density = 100%+ even with just 2 cars on the road.
}
```

---

## 2. Pathfinding Algorithms — Strategy Pattern

All three algorithms share a single interface, making them hot-swappable at runtime:
```java
public interface RoutingStrategy {
    List<Road> calculateRoute(Intersection start, Intersection target, Vehicle vehicle);
}
```

### `AStarRouting.java` — Heuristic-Guided Search
**Purpose:** Finds the shortest path by balancing actual distance travelled (g-cost) with an estimated distance remaining (h-cost). Best all-round performer.

**Core Logic Walkthrough:**
```java
// 1. Priority Queue — always expands the node with lowest F = G + H first
PriorityQueue<NodeRecord> openList = new PriorityQueue<>(
    Comparator.comparingDouble(n -> n.estimatedTotalCost));

// 2. Track best known cost to every visited node
Map<Intersection, Double> costSoFar = new HashMap<>();

// 3. Seed with the start node (G=0, H=Euclidean to target)
openList.add(new NodeRecord(start, null, null, 0, heuristic(start, target)));
costSoFar.put(start, 0.0);

while (!openList.isEmpty()) {
    NodeRecord current = openList.poll();           // 4. Expand most promising node

    if (current.node == target) return buildPath(current); // 5. Done — trace back

    for (Road road : current.node.getConnectedRoads()) {
        if (road.isUnderConstruction()) continue;   // 6. Skip blocked roads

        // 7. Dynamic traffic penalty: 1.0 (empty) → 5.0 (fully congested)
        double trafficPenalty = 1.0 + (road.getTrafficDensity() * 4.0);

        // 8. New G-cost = distance so far + penalised road length
        double newCost = costSoFar.get(current.node) + (road.getLength() * trafficPenalty);

        if (!costSoFar.containsKey(road.getEnd()) || newCost < costSoFar.get(road.getEnd())) {
            costSoFar.put(road.getEnd(), newCost);
            double priority = newCost + heuristic(road.getEnd(), target); // F = G + H
            openList.add(new NodeRecord(road.getEnd(), current, road, newCost, priority));
        }
    }
}
```

### `DijkstraRouting.java` — Exhaustive Shortest Path
**Purpose:** A* with heuristic = 0. Explores all directions equally. Guarantees the optimal path but does more work — slower on large maps.

### `GreedyRouting.java` — Best-First Search
**Purpose:** Uses only the heuristic (straight-line distance to goal), ignoring g-cost and traffic. Very fast but takes suboptimal routes and can get stuck in local minima.

---

## 3. Simulation Entities

### `Vehicle.java`
**Purpose:** A car/ambulance on the map. Holds its own path, position state, and routing brain.

**Key Fields:**

| Field | Type | Purpose |
|---|---|---|
| `ticksAlive` | `int` | Benchmark counter — total simulation steps taken |
| `totalDistanceTraveled` | `double` | Accumulated road length for analytics |
| `hasRerouted` | `boolean` | `true` after smart detour — renders purple |
| `isWaitingOrSlow` | `boolean` | `true` when stuck at red light or jammed road — renders red |
| `isAmbulance` | `boolean` | Ambulances ignore red lights and move at 2× speed |
| `justRerouted` | `boolean` | *(new)* `true` for ~80 frames after a road-block forced reroute — renders as flashing cyan |
| `rerouteFlashTimer` | `int` | *(new)* Countdown for the cyan flash animation |
| `isBottleneckCar` | `boolean` | *(new)* `true` for cars spawned by Force Bottleneck — renders as deep magenta |

**`moveNextTick()` Walkthrough:**
```java
public void moveNextTick() {
    Road nextRoad = currentPath.get(0);

    // SMART DETOUR: At every intersection, check if road ahead is blocked or >70% full
    if (roadProgress == 0.0 && (nextRoad.getTrafficDensity() >= 0.7 || nextRoad.isUnderConstruction())) {
        this.currentPath = routingBrain.calculateRoute(currentLocation, targetDestination, this);
        if (currentPath == null || currentPath.isEmpty()) { isWaitingOrSlow = true; return; }
        nextRoad = currentPath.get(0);
        hasRerouted = true;  // → render purple
    }

    // TRAFFIC LIGHT LOGIC (ambulances bypass)
    if (roadProgress == 0.0) {
        boolean roadIsHorizontal = Math.abs(nextRoad.getEnd().getX() - nextRoad.getStart().getX())
                                 > Math.abs(nextRoad.getEnd().getY() - nextRoad.getStart().getY());
        if (!isAmbulance && roadIsHorizontal != currentLocation.isHorizontalGreen) {
            isWaitingOrSlow = true;  // → render red, stop this tick
            return;
        }
        nextRoad.enterRoad(); // increments density counter
    }

    // SPEED: ambulance = 2×, weather multiplier applied globally
    double speed = (isAmbulance ? 30.0 : 15.0) * SmartCityGUI.getGlobalSpeedMultiplier();
    double progressIncrement = speed / nextRoad.getLength();

    totalDistanceTraveled += (progressIncrement * nextRoad.getLength()); // benchmark tracking
    roadProgress += progressIncrement;

    if (roadProgress >= 1.0) {
        this.currentLocation = nextRoad.getEnd();
        nextRoad.exitRoad(); // decrements density counter
        currentPath.remove(0);
        roadProgress = 0.0;
    } else {
        // Interpolate pixel position for smooth animation
        this.drawX = nextRoad.getStart().getX() + (nextRoad.getEnd().getX()-nextRoad.getStart().getX()) * roadProgress;
        this.drawY = nextRoad.getStart().getY() + (nextRoad.getEnd().getY()-nextRoad.getStart().getY()) * roadProgress;
    }
}
```

### `DeliveryTruck.java`
**Purpose:** Extends `Vehicle`. Implements a Travelling Salesman Problem (TSP) approximation using Nearest Neighbour heuristic to visit multiple delivery stops in an efficient order before routing to each with the selected algorithm.

### `SpawnRecord.java`
**Purpose:** A plain data class holding `(start, target, spawnDelayTicks, isAmbulance)`. Used to stagger vehicle spawns over time so the simulation builds gradually rather than all vehicles appearing at once.

---

## 4. UI Layer

### `SmartCityGUI.java` — Main Dashboard
**Purpose:** The root JFrame. Owns the city graph, vehicle list, spawn records, and the master `javax.swing.Timer` loop.

**Control Panel (2 rows):**
- **Row 1 — Selectors:** Routing algorithm, Weather, Animation speed (slider), Simulation speed (1×–20×), Car count (100/200/400)
- **Row 2 — Buttons:** Start Simulation, Pause, Reset Roads, Run Benchmark, Race Mode, Delivery Truck, Step Visualizer, **Force Bottleneck** *(new)*

**Master Timer Loop:**
```java
timer = new Timer(33ms, e -> {
    for (int step = 0; step < stepsPerTick; step++) {  // stepsPerTick = 1/2/5/10/20
        globalTick++;
        simulationPanel.advanceTick();    // advances flash animations
        if (globalTick % 60 == 0) toggleAllTrafficLights();
        spawnDueVehicles();
        for (Vehicle v : activeVehicles) if (!v.isFinished()) v.moveNextTick();
    }
    updateSidebarStats();
    simulationPanel.repaint();
    if (allFinished()) exportCSVAndStop();
});
```

**`stopCurrentOperation()`** *(new)*
```java
private void stopCurrentOperation() {
    if (timer != null && timer.isRunning()) timer.stop(); // cancel any running sim
    isVisualSimRunning = false;
    setControlsEnabled(true);
}
// Called at the start of EVERY button action so starting a new operation
// always cleanly cancels the previous one — no state conflicts.
```

**`forceBottleneckDemo()`** *(new — guaranteed congestion)*
```java
// Step 1: Find the geometric centre node of the grid (natural chokepoint)
// Step 2: setCapacity(1) on ALL roads touching that node
//         → density = currentCars / 1 → hits 100% with just 2 cars
// Step 3: Spawn 150 cars left-half → right-half + 50 reverse
//         → all paths converge on centre → INSTANT JAM
// Step 4: CONGESTION ALERT banner fires on the map within ~1 second
```

**Car Count Selector** *(new)*
- Combo box: `100 Cars / 200 Cars / 400 Cars`
- Regenerates `spawnRecords` immediately on change
- Also used by `startVisualSimulation()` when starting a fresh sim

---

### `SimulationPanel.java` — Map Renderer
**Purpose:** Handles all `Graphics2D` drawing. Auto-scales the graph to always fill the available panel area.

**Auto-scaling (new):**
```java
private void computeScale() {
    // Called every paintComponent() so the graph always fits regardless of window size
    int minX, maxX, minY, maxY = /* scan all nodes */;
    int margin = 50;
    scale   = Math.min((width  - 2*margin) / graphWidth,
                       (height - 2*margin) / graphHeight);
    offsetX = margin - minX * scale;
    offsetY = margin - minY * scale;
}
```

**Road colour scheme (density-based):**
| Density | Colour | Thickness |
|---|---|---|
| < 40% | Grey | 2px |
| 40–70% | Yellow | 3.5px |
| 70–100% | Red | 5px |
| 100%+ (jam) | Dark Red | 7px |
| Blocked | Orange dashed | 3.5px |

**Congestion % labels (new):** When density ≥ 50%, the exact percentage is drawn at the road's midpoint so the teacher can read exact congestion values.

**CONGESTION ALERT banner (new):** A pulsing banner appears at the top of the map whenever any road reaches ≥ 70% density. Shows peak density and the road name. Turns deep red and says "TRAFFIC JAM DETECTED!" at 100%+.

**Road-click → Reroute Highlight (new):**
```java
// When user clicks a road to block it:
// 1. Scan all active vehicles whose NEXT road is the blocked road
// 2. Set v.justRerouted = true, v.rerouteFlashTimer = 80
// 3. Those vehicles render as flashing cyan/yellow for ~80 frames
// 4. Console logs: "🔄 REROUTING: N vehicle(s) forced onto new path"
```

**Vehicle colour legend** — now rendered in the **sidebar** (not on the map):

| Colour | Meaning |
|---|---|
| Blue | Normal moving car |
| Red | Stuck at light / congested road |
| Purple | Has previously rerouted |
| Cyan (flash) | Just forced to reroute by road block |
| Deep Magenta | Bottleneck demo car |
| Orange | Delivery truck |
| Red↔Blue (flash) | Ambulance |

---

### `RaceModeGUI.java` — Algorithm Race
**Purpose:** Splits the screen into 3 `SimulationPanel`s, each running an independent copy of the city graph with the same seeded vehicle spawns, so A*, Dijkstra, and Greedy compete head-to-head.

**New features added:**
- **Car count selector:** 100 / 200 / 400 cars before race starts
- **Sim speed combo:** 1× / 2× / 5× / 10× / 20× (`stepsPerTick` mechanism)
- **Live wall clock:** Shows `⏱ MM:SS` during the race
- **Finish-time tracking:** Records exact timestamp when each algorithm delivers all cars
- **Post-race analysis dialog:** Triggered automatically when all 3 finish

**Post-Race Analysis:**
```
| Algorithm | Finish Time | Avg Ticks/Car | Avg Distance/Car |
|-----------|-------------|---------------|-----------------|
| A*        | 1:23.4      | 84.2          | 210             |
| Dijkstra  | 1:45.1      | 96.7          | 208             |
| Greedy    | 2:01.8      | 112.3         | 245             |

Conclusion:
- 🥇 Fastest completion: A* (1:23.4)
- 📏 Shortest avg path: Dijkstra (208)
- A*: balanced heuristic — best overall
- Dijkstra: optimal paths but more exploration work
- Greedy: fast start but suboptimal routes, gets stuck
```

---

### `AlgorithmVisualizerGUI.java` — Step-by-Step Debugger
**Purpose:** An educational tool that runs a single pathfinding algorithm one step at a time, showing the Priority Queue state and exactly which nodes are being expanded.

**New features (full rewrite):**
- **Algorithm selector:** A*, Dijkstra, Greedy — each correctly implements its own f-cost function
  - A*: `f = g + h`
  - Dijkstra: `f = g` (h=0)
  - Greedy: `f = h` (ignores g)
- **Auto-scaling graph** — always fills the panel (same `computeScale()` logic)
- **Toggle Auto Play / Pause** — timer stops automatically when done
- **↺ Reset button** — picks new random nodes and restarts
- **Richer explanation panel** — shows `g=X h=Y f=Z` for every neighbour expanded
- **Priority Queue** — next-to-expand node marked with `▶`
- **Colour legend** at bottom of window

**Node colours during visualisation:**
| Colour | State |
|---|---|
| Green | Start node |
| Red | Target node |
| Yellow | Currently being expanded |
| Purple | Already visited (closed set) |
| Blue | In frontier (open set / priority queue) |
| Green line | Final path traced |

---

## 5. Benchmarking Engine

### `BenchmarkEngine.java`
**Purpose:** Runs a headless (no UI) fast-forward simulation of all 3 algorithms on the same spawn records for fair comparison. Called by `runAllBenchmarksHeadless()` in a `SwingWorker` background thread so the GUI never freezes.

### `BenchmarkResult.java`
Holds per-algorithm summary statistics: algorithm name, average ticks per vehicle, average distance, and reroute count.

### `ResultsChartPanel.java`
Renders bar charts comparing the 3 algorithms after a benchmark run.

### `CSVExporter.java`
**Purpose:** File I/O utility. Appends per-vehicle stats to `benchmark_results.csv` in append mode after every simulation.
```java
FileWriter fw = new FileWriter("benchmark_results.csv", true);  // append=true
pw.printf("%s,%s,%s,%b,%d,%f\n", algorithm, start, target, isAmbulance, ticks, distance);
```
*The file can be opened in Excel/Google Sheets for further analysis.*

---

## 6. Map Generation & Loading

### `MapLoader.java`
Reads `complex_grid_map.txt` and builds the `Map<Integer, Intersection>` graph by parsing `NODE` and `EDGE` lines.

### `GridMapGenerator.java`
Generates `complex_grid_map.txt` — a 12×12 grid of 144 nodes with randomised road lengths (10–40 units), capacities (5–19 vehicles), and occasional pre-blocked roads.

---

## 7. Key Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `RoutingStrategy` interface | Swap A*/Dijkstra/Greedy at runtime without changing Vehicle |
| **Observer (callback)** | `SimulationPanel.setOnRoadBlocked()` | Decouple road-click detection from GUI logic |
| **Template Method** | `Vehicle.moveNextTick()` | Common movement logic, subclasses (Taxi, DeliveryTruck) extend it |
| **SwingWorker** | `runAllBenchmarksHeadless()` | Keep UI responsive during heavy computation |
| **MVC** | Overall architecture | Model = graph/vehicles, View = SimulationPanel, Controller = SmartCityGUI |
