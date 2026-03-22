public class Intersection {
    private String name;
    private String lightState;
    private int timer;

    public Intersection(String name) {
        this.name = name;
        this.lightState = "GREEN";
        this.timer = 0;
    }

    public void updateLight() {
        timer++;
        if (timer >= 5) { // Light changes every 5 "ticks" of the engine
            lightState = lightState.equals("GREEN") ? "RED" : "GREEN";
            timer = 0;
        }
    }

    public String getName() {
        return name;
    }

    public String getLightState() {
        return lightState;
    }

    // Crucial for HashMap keys in Graph Theory
    @Override
    public String toString() {
        return name;
    }
}