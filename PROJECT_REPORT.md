# Smart City Traffic Simulator & Routing Algorithm Benchmark
**Project Report / Documentation**

## 1. Project Overview
The **Smart City Traffic Simulator** is an advanced, interactive Java-based application designed to simulate real-world urban traffic. The primary objective is to apply and visualize **Graph Theory** concepts by benchmarking three distinct pathfinding algorithms (A*, Dijkstra, and Greedy Best-First Search) under dynamic, unpredictable conditions.

The system simulates a complex urban grid network where vehicles act autonomously. It demonstrates how routing algorithms behave when introduced to real-world penalties such as heavy traffic congestion, red lights, road construction, and dynamic weather.

## 2. Core Architecture & Graph Theory Application
The city map is constructed as a mathematical Graph:
* **Nodes (Intersections):** Represent junctions in the city. Some nodes are assigned specific landmarks (e.g., *Graphic Era University*, *Clock Tower*).
* **Edges (Roads):** Directed/undirected connections between nodes. Each edge holds a `weight` (physical distance) and dynamic penalties (Traffic Density, Construction status).

## 3. Pathfinding Algorithms Implemented
The application evaluates three different graph-traversal strategies:

1. **A* (A-Star) Routing**: 
   * *Concept*: Uses both the actual distance traveled from the start (G-cost) and an estimated distance to the target (H-cost/Heuristic) combined with dynamic traffic penalties.
   * *Result*: The most intelligent algorithm. It dynamically reroutes to avoid traffic jams and road closures while still heading generally toward the destination.
2. **Dijkstra's Algorithm**:
   * *Concept*: Exhaustively explores all possible paths from the starting node to guarantee the absolute lowest-penalty route.
   * *Result*: Always finds the optimal route, but is computationally heavier and explores far more nodes than A*.
3. **Greedy Best-First Search**:
   * *Concept*: Only considers the physical straight-line distance to the target (Heuristic). It ignores all traffic density and construction penalties.
   * *Result*: Fast calculation, but highly inefficient in practice as it frequently drives straight into severe traffic jams or blocked roads.

## 4. Dynamic Traffic Engine
To make the simulation realistic, the engine does not just find a static path; it simulates the passage of time:
* **Staggered Spawning**: 400 vehicles are not spawned instantly, but stagger-spawned over a set duration to simulate rush hour traffic organically.
* **Traffic Lights**: A global timer toggles horizontal and vertical lights at intersections. Vehicles actively scan intersection states and wait at red lights to prevent collisions.
* **Dynamic Recalculation**: If a vehicle approaches a road that has exceeded 70% traffic capacity, or a road that was suddenly closed, the algorithm will dynamically calculate a new "Smart Detour".

## 5. Advanced Interactive Features
During development, the following premium features were engineered to elevate the project:

### 🎮 Interactive Map Interventions
Users can click on any road segment during a live simulation to instantly mark it as "Under Construction". Vehicles currently on the map will detect this closure and visually recalculate their routes.

### 🚑 Emergency Vehicle System (Ambulance Mode)
5% of spawned vehicles are classified as Ambulances.
* They are visually distinct, flashing Red and Blue.
* They move at 2x the normal speed.
* They possess elevated permissions, allowing them to ignore traffic jams and blow through red lights.

### ⛈️ Dynamic Weather Engine
A global weather control system that mathematically alters the simulation:
* **Sunny**: Baseline speed multipliers (1.0x).
* **Heavy Rain**: Reduces global traffic speed by 40% (0.6x).
* **Snow Storm**: Cripples global traffic speed by 70% (0.3x), causing massive and rapid traffic congestion.

### 🏆 Split-Screen Race Mode
A dedicated "Algorithm Showdown" dashboard. It creates three independent, deep-copied graph networks and runs all three algorithms (A*, Dijkstra, Greedy) simultaneously with identical spawn parameters. This allows for a direct, visually provable comparison of how each algorithm handles the exact same urban crisis.

### 📊 Automated CSV Analytics Export
For academic data collection, the system utilizes a Headless Benchmark Engine. Once a simulation completes, a `CSVExporter` automatically dumps the telemetry data (Vehicle ID, Algorithm Used, Time Alive, Total Distance, Target Status) to a `benchmark_results.csv` file. This allows the data to be graphed using Excel or Python for final report analysis.

## 6. User Interface (UI/UX)
The application was styled utilizing a custom **"Modern Dark Mode GPS"** aesthetic.
* Replaced standard Java Swing metal frames with System LookAndFeel.
* Utilized high-contrast neon colors to categorize vehicle states:
  * **Cyan**: Normal traffic flow.
  * **Red**: Vehicle stopped (Red Light / Traffic Jam).
  * **Magenta**: Vehicle dynamically rerouting to avoid an obstacle.
* **Live Telemetry Sidebar**: Tracks moving cars, stopped cars, and overall City Congestion percentages in real-time.
* **Educational Console**: A live-updating text area that explains algorithmic decisions and system events to the user as they happen.

## 7. Technologies Used
* **Language**: Java (JDK 8+)
* **GUI Framework**: Java Swing / AWT Graphics2D
* **Data Structures**: HashMaps, PriorityQueues, ArrayLists
* **Design Patterns**: Strategy Pattern (Interchangeable Routing Algorithms)
