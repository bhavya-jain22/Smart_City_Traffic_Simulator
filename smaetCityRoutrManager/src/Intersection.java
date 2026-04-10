import java.util.ArrayList;
import java.util.List;

public class Intersection {
    private int id;
    private String name;
    private int x, y; // VIVA FLEX: Coordinates needed for A* Euclidean Heuristic
    private List<Road> connectedRoads;

    public Intersection(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.connectedRoads = new ArrayList<>();
    }

    public void addConnectedRoad(Road road) {
        this.connectedRoads.add(road);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<Road> getConnectedRoads() {
        return connectedRoads;
    }
}