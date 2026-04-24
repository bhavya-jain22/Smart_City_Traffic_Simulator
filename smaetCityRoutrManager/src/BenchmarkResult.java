public class BenchmarkResult {
    public final String algorithmName;
    public final double averageDistance;
    public final double averageTicks;
    
    public BenchmarkResult(String algorithmName, double averageDistance, double averageTicks) {
        this.algorithmName = algorithmName;
        this.averageDistance = averageDistance;
        this.averageTicks = averageTicks;
    }
}
