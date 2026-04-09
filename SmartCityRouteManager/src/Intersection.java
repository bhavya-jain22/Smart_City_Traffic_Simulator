public class Intersection {
    private String name;
    private int x, y; 
    private String lightState;
    private int timer;

    public Intersection(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.lightState = "GREEN";
        this.timer = 0;
    }

    public void updateLight() {
        timer++;
        if (timer >= 5) {
            lightState = lightState.equals("GREEN") ? "RED" : "GREEN";
            timer = 0;
        }
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
    public String getLightState() { 
        return lightState;
    }
    
    
    public String toString() { 
        return name;
    }
}
