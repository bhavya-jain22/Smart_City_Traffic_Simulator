import java.util.*;

public class HeadLessEngine {
    // VIVA FLEX: Global State Variable to mutate all edge weights at once
    public static double weatherMultiplier = 1.0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Starting Smart City Engine ---");

        // 1. Load the Map
        Map<Integer, Intersection> cityGraph = MapLoader.loadMap("dehradun_map.txt");
        System.out.println("✅ Map Loaded Successfully.");

        // 2. Setup Routing and Vehicles
       // 2. Setup Routing and Vehicles
        RoutingStrategy aStar = new AStarRouting();
        List<Vehicle> activeVehicles = new ArrayList<>();
        
        System.out.println("🚗🚦 INITIATING RUSH HOUR TRAFFIC...");
        
        // Spawn multiple vehicles with overlapping routes to force traffic!
        activeVehicles.add(new Vehicle(cityGraph.get(1), cityGraph.get(8), aStar)); // GEU to Clock Tower
        activeVehicles.add(new Taxi(cityGraph.get(2), cityGraph.get(6), aStar));    // Clement Town to Shimla Bypass
        activeVehicles.add(new Vehicle(cityGraph.get(3), cityGraph.get(7), aStar)); // Transport Nagar to Vasant Vihar
        activeVehicles.add(new Vehicle(cityGraph.get(5), cityGraph.get(8), aStar)); // Prem Nagar to Clock Tower
        activeVehicles.add(new Taxi(cityGraph.get(1), cityGraph.get(6), aStar));    // GEU to Shimla Bypass
        activeVehicles.add(new Vehicle(cityGraph.get(2), cityGraph.get(8), aStar)); // Clement Town to Clock Tower
        System.out.println("🚗 Spawned Regular Vehicle (Route: 1 -> 8)");
        System.out.println("🚖 Spawned Premium Taxi (Route: 2 -> 11)");

        // 3. The Main Simulation Tick Loop
        // 3. The Main Simulation Tick Loop
        int tick = 0;
        while (tick < 30) { 
            System.out.println("\n--- Tick: " + tick + " ---");

            // GLOBAL EVENT TRIGGER
            if (tick == 3) {
                System.out.println("🚨 STORM INCOMING! Weather Multiplier is now 2.5 🚨");
                weatherMultiplier = 2.5;
            }

            // Move Vehicles and check if they are done
            boolean allFinished = true;
            for (Vehicle v : activeVehicles) {
                Intersection before = v.getCurrentLocation();
                v.moveNextTick();
                Intersection after = v.getCurrentLocation();
                
                if (before != after) {
                    String type = v instanceof Taxi ? "Taxi" : "Car";
                    System.out.println(type + " arrived at: " + after.getName());
                }

                // If even ONE vehicle is still driving, we are not finished
                if (!v.isFinished()) {
                    allFinished = false;
                }
            }

            // VIVA FLEX: Auto-Shutdown Logic
            if (allFinished) {
                System.out.println("\n🏁 All vehicles have reached their destinations. Shutting down engine.");
                break; // This stops the empty ticks!
            }

            Thread.sleep(800); 
            tick++;
        }
        System.out.println("--- Simulation Complete ---");
    }
}