public class vehicle {

    private int x;
    private int y;
    private double speed;
    private boolean isMoving;

    // Constructor
    public vehicle(int x, int y, double speed, boolean isMoving) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.isMoving = isMoving;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isMoving() {
        return isMoving;
    }

    // Setters
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }
}