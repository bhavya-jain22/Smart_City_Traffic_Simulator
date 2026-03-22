public class intersection {

    private int x;
    private int y;
    private String currentState; // "RED" or "GREEN"

    // Constructor
    public intersection(int x, int y, String currentState) {
        this.x = x;
        this.y = y;
        this.currentState = currentState;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getCurrentState() {
        return currentState;
    }

    // Setters
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
}
