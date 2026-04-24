import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CSVExporter {

    public static void exportData(String filename, String algorithm, List<Vehicle> vehicles) {
        boolean fileExists = new java.io.File(filename).exists();
        
        try (FileWriter fw = new FileWriter(filename, true);
             PrintWriter pw = new PrintWriter(fw)) {
             
            // Write CSV Header if file doesn't exist
            if (!fileExists) {
                pw.println("Algorithm,StartNode,EndNode,IsAmbulance,TicksAlive,TotalDistance,Status");
            }
            
            for (Vehicle v : vehicles) {
                String status = v.isFinished() ? "Arrived" : "Stuck";
                String startName = v.currentPath != null && !v.currentPath.isEmpty() ? v.currentPath.get(0).getStart().getName() : "Unknown";
                String targetName = v.targetDestination != null ? v.targetDestination.getName() : "Unknown";
                
                pw.printf("%s,%s,%s,%b,%d,%.2f,%s%n",
                        algorithm,
                        startName,
                        targetName,
                        v.isAmbulance,
                        v.ticksAlive,
                        v.totalDistanceTraveled,
                        status
                );
            }
            
            System.out.println("Data exported to " + filename);
            
        } catch (IOException e) {
            System.err.println("Failed to export CSV: " + e.getMessage());
        }
    }
}
