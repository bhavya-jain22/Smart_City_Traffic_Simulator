public class SpawnRecord {
    public final Intersection start;
    public final Intersection target;
    public final int spawnDelayTicks;

    public SpawnRecord(Intersection start, Intersection target, int spawnDelayTicks) {
        this.start = start;
        this.target = target;
        this.spawnDelayTicks = spawnDelayTicks;
    }
}
