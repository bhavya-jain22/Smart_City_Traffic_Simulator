import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BenchmarkEngine {

    public static BenchmarkResult runBenchmark(String algoName, RoutingStrategy strategy, Map<Integer, Intersection> cityGraph, List<SpawnRecord> spawns) {
        // Reset edge traffics to ensure clean slate
        for (Intersection node : cityGraph.values()) {
            for (Road r : node.getConnectedRoads()) {
                while (r.getTrafficDensity() > 0) {
                    r.exitRoad();
                }
            }
        }

        List<Vehicle> allVehicles = new ArrayList<>();
        List<Vehicle> activeVehicles = new ArrayList<>();
        int tick = 0;

        // Simulation Loop
        while (true) {
            // Spawn vehicles
            for (SpawnRecord record : spawns) {
                if (record.spawnDelayTicks == tick) {
                    Vehicle v = new Vehicle(record.start, record.target, strategy, record.isAmbulance);
                    allVehicles.add(v);
                    activeVehicles.add(v);
                }
            }

            // Move vehicles
            boolean anyMoving = false;
            for (Vehicle v : activeVehicles) {
                if (!v.isFinished()) {
                    v.moveNextTick();
                    anyMoving = true;
                }
            }

            tick++;

            // If we have spawned all vehicles, and none are moving, we are done
            if (!anyMoving && tick > getHighestSpawnTick(spawns)) {
                break;
            }

            // Fallback to prevent infinite loops if something gets totally stuck
            if (tick > 50000) {
                System.err.println("Benchmark " + algoName + " hit 50k ticks, forcing stop.");
                break;
            }
        }

        double totalDist = 0;
        int totalTicks = 0;
        for (Vehicle v : allVehicles) {
            totalDist += v.totalDistanceTraveled;
            totalTicks += v.ticksAlive;
        }

        double avgDist = totalDist / allVehicles.size();
        double avgTicks = (double) totalTicks / allVehicles.size();

        CSVExporter.exportData("benchmark_results.csv", algoName, allVehicles);

        return new BenchmarkResult(algoName, avgDist, avgTicks);
    }

    private static int getHighestSpawnTick(List<SpawnRecord> spawns) {
        int max = 0;
        for (SpawnRecord r : spawns) {
            if (r.spawnDelayTicks > max) {
                max = r.spawnDelayTicks;
            }
        }
        return max;
    }
}
