public class Road {
    private Intersection startNode;
    private Intersection endNode;
    private double length;
    private int trafficDensity;

    public Road(Intersection startNode, Intersection endNode, double length) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.length = length;
        this.trafficDensity = 0;
    }

    public Intersection getStartNode() {
        return startNode;
    }

    public Intersection getEndNode() {
        return endNode;
    }

    public double getLength() {
        return length;
    }

    public int getTrafficDensity() {
        return trafficDensity;
    }

    public void addTraffic() {
        this.trafficDensity++;
    }

    public void removeTraffic() {
        if (this.trafficDensity > 0)
            this.trafficDensity--;
    }

    @Override
    public String toString() {
        return startNode.getName() + "->" + endNode.getName();
    }
}