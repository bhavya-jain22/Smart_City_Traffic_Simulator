import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class GridMapGenerator {

    public static void main(String[] args) {
        int width = 12;
        int height = 12;
        int nodeSpacing = 4;
        String filename = "complex_grid_map.txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("# Generated Complex Grid Map");
            writer.println("# NODE,id,name,x,y");
            writer.println("# EDGE,startId,endId,length,capacity,isUnderConstruction,potholes");
            writer.println();

            // Generate Nodes
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    int id = r * width + c + 1; // 1-indexed
                    String name = "Node_" + id;
                    
                    // Assign special Dehradun University/Landmark names
                    if (r == 2 && c == 2) name = "Graphic Era Univ";
                    else if (r == 10 && c == 10) name = "Clock Tower";
                    else if (r == 2 && c == 10) name = "Clement Town";
                    else if (r == 10 && c == 2) name = "ISBT Hub";
                    else if (r == 6 && c == 6) name = "Rajpur Road";
                    else if (r == 8 && c == 3) name = "Prem Nagar";
                    else if (r == 5 && c == 10) name = "Pacific Mall";
                    else if (r == 3 && c == 7) name = "Paltan Bazaar";
                    
                    int x = c * nodeSpacing;
                    int y = r * nodeSpacing;
                    writer.println("NODE," + id + "," + name + "," + x + "," + y);
                }
            }

            writer.println();
            Random rand = new Random(42); // Fixed seed for reproducible grid

            // Generate Edges
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    int currentId = r * width + c + 1;

                    // Right Edge
                    if (c < width - 1) {
                        int rightId = currentId + 1;
                        writeEdgePair(writer, rand, currentId, rightId);
                    }

                    // Bottom Edge
                    if (r < height - 1) {
                        int bottomId = currentId + width;
                        writeEdgePair(writer, rand, currentId, bottomId);
                    }
                }
            }
            System.out.println("Generated complex map with " + (width * height) + " nodes at " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeEdgePair(PrintWriter writer, Random rand, int id1, int id2) {
        // Base length + random variance
        double length1 = 300 + rand.nextInt(500); 
        double length2 = length1; // bidirectional distance is usually same

        int capacity1 = 5 + rand.nextInt(15);
        int capacity2 = 5 + rand.nextInt(15);

        boolean underConstruction1 = rand.nextDouble() < 0.05; // 5% chance
        boolean underConstruction2 = underConstruction1 ? true : rand.nextDouble() < 0.05;

        int potholes1 = rand.nextInt(3); // 0 to 2
        int potholes2 = rand.nextInt(3);

        writer.println("EDGE," + id1 + "," + id2 + "," + length1 + "," + capacity1 + "," + underConstruction1 + "," + potholes1);
        writer.println("EDGE," + id2 + "," + id1 + "," + length2 + "," + capacity2 + "," + underConstruction2 + "," + potholes2);
    }
}
