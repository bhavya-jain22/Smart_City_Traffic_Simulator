public class road {

    private int startX, startY;
    private int endX, endY;
    private int trafficDensity;

    // Constructor
    public road(int startX, int startY, int endX, int endY, int trafficDensity) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.trafficDensity = trafficDensity;
    }

    // Getters
    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public int getTrafficDensity() {
        return trafficDensity;
    }

    // Setters
    public void setTrafficDensity(int trafficDensity) {
        this.trafficDensity = trafficDensity;
    }
}