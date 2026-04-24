public class SpawnRecord {
    public final Intersection start;
    public final Intersection target;
    public final int spawnDelayTicks;
    public final boolean isAmbulance;

    public SpawnRecord(Intersection start, Intersection target, int spawnDelayTicks, boolean isAmbulance) {
        this.start = start;
        this.target = target;
        this.spawnDelayTicks = spawnDelayTicks;
        this.isAmbulance = isAmbulance;
    }
}
