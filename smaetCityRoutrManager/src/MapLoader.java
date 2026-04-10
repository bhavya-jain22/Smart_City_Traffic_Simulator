import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class MapLoader {
    // VIVA FLEX: Using BufferedReader for memory-efficient I/O parsing
    public static Map<Integer, Intersection> loadMap(String filename) {
        Map<Integer, Intersection> cityGraph = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;

                String[] p = line.split(",");

                if (p[0].equals("NODE")) {
                    Intersection node = new Intersection(Integer.parseInt(p[1]), p[2], Integer.parseInt(p[3]),
                            Integer.parseInt(p[4]));
                    cityGraph.put(node.getId(), node);
                } else if (p[0].equals("EDGE")) {
                    Intersection start = cityGraph.get(Integer.parseInt(p[1]));
                    Intersection end = cityGraph.get(Integer.parseInt(p[2]));
                    Road r = new Road(start, end, Double.parseDouble(p[3]), Integer.parseInt(p[4]),
                            Boolean.parseBoolean(p[5]), Integer.parseInt(p[6]));
                    start.addConnectedRoad(r);
                }
            }
        } catch (Exception e) {
            System.err.println("Error Loading Map: " + e.getMessage());
        }
        return cityGraph;
    }
}