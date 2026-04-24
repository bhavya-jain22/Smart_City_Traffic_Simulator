import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SimulationPanel extends JPanel {
    private Map<Integer, Intersection> cityGraph;
    private List<Vehicle> activeVehicles;
    private Consumer<String> logger;

    // Scale factors for translating graph coordinates to screen pixels
    private final int SCALE = 16;
    private final int OFFSET_X = 60;
    private final int OFFSET_Y = 60;

    public SimulationPanel(Map<Integer, Intersection> cityGraph, List<Vehicle> activeVehicles, Consumer<String> logger) {
        this.cityGraph = cityGraph;
        this.activeVehicles = activeVehicles;
        this.logger = logger;
        setBackground(new Color(240, 248, 255)); // Alice Blue background

        // Mouse Listener to block/unblock roads
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        if (cityGraph == null) return;
        
        Road closestRoad = null;
        double minDistance = Double.MAX_VALUE;

        // Find the road segment closest to the click
        for (Intersection node : cityGraph.values()) {
            for (Road road : node.getConnectedRoads()) {
                int x1 = road.getStart().getX() * SCALE + OFFSET_X;
                int y1 = road.getStart().getY() * SCALE + OFFSET_Y;
                int x2 = road.getEnd().getX() * SCALE + OFFSET_X;
                int y2 = road.getEnd().getY() * SCALE + OFFSET_Y;

                double dist = pointToSegmentDistance(mouseX, mouseY, x1, y1, x2, y2);
                if (dist < minDistance && dist < 10) { // Click within 10 pixels
                    minDistance = dist;
                    closestRoad = road;
                }
            }
        }

        if (closestRoad != null) {
            boolean isUnderConst = closestRoad.isUnderConstruction();
            closestRoad.setUnderConstruction(!isUnderConst);
            
            // also toggle the reverse edge if it exists
            for (Road reverse : closestRoad.getEnd().getConnectedRoads()) {
                if (reverse.getEnd() == closestRoad.getStart()) {
                    reverse.setUnderConstruction(!isUnderConst);
                }
            }
            
            if (!isUnderConst && logger != null) {
                logger.accept("INTERACTION: You blocked the road between " + closestRoad.getStart().getName() + " and " + closestRoad.getEnd().getName() + ". Watch vehicles dynamically recalculate to avoid it!");
            } else if (isUnderConst && logger != null) {
                logger.accept("INTERACTION: You unblocked the road. Traffic can flow freely again.");
            }
            repaint();
        }
    }

    private double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Roads (Edges)
        g2d.setStroke(new BasicStroke(3.0f));

        if (cityGraph != null) {
            for (Intersection node : cityGraph.values()) {
                int startX = node.getX() * SCALE + OFFSET_X;
                int startY = node.getY() * SCALE + OFFSET_Y;

                for (Road road : node.getConnectedRoads()) {
                    Intersection endNode = road.getEnd();
                    int endX = endNode.getX() * SCALE + OFFSET_X;
                    int endY = endNode.getY() * SCALE + OFFSET_Y;
                    
                    if (road.isUnderConstruction()) {
                        g2d.setColor(new Color(255, 140, 0)); // Dark Orange for construction block
                    } else if (road.getTrafficDensity() > 0.6) {
                        g2d.setColor(Color.RED); // Heavy traffic
                    } else if (road.getPotholeCount() > 0) {
                        g2d.setColor(new Color(139, 69, 19)); // Saddle Brown for potholes
                    } else {
                        g2d.setColor(Color.LIGHT_GRAY);
                    }
                    g2d.drawLine(startX, startY, endX, endY);
                }
            }

            // 2. Draw Intersections (Nodes) & Traffic Lights
            for (Intersection node : cityGraph.values()) {
                int cx = node.getX() * SCALE + OFFSET_X;
                int cy = node.getY() * SCALE + OFFSET_Y;

                // Node Center
                g2d.setColor(Color.BLACK);
                g2d.fillOval(cx - 3, cy - 3, 6, 6);

                // Draw Traffic Light Indicators (Horizontal & Vertical)
                if (node.isHorizontalGreen) {
                    g2d.setColor(Color.GREEN);
                    g2d.fillRect(cx - 6, cy - 2, 2, 4); // left
                    g2d.fillRect(cx + 4, cy - 2, 2, 4); // right
                    
                    g2d.setColor(Color.RED);
                    g2d.fillRect(cx - 2, cy - 6, 4, 2); // top
                    g2d.fillRect(cx - 2, cy + 4, 4, 2); // bottom
                } else {
                    g2d.setColor(Color.RED);
                    g2d.fillRect(cx - 6, cy - 2, 2, 4); // left
                    g2d.fillRect(cx + 4, cy - 2, 2, 4); // right
                    
                    g2d.setColor(Color.GREEN);
                    g2d.fillRect(cx - 2, cy - 6, 4, 2); // top
                    g2d.fillRect(cx - 2, cy + 4, 4, 2); // bottom
                }

                // Draw Special Landmark Names
                if (!node.getName().startsWith("Node_")) {
                    g2d.setColor(new Color(0, 0, 139)); // Dark Blue
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString(node.getName(), cx + 8, cy - 8);
                }
            }
        }

        // 3. Draw Vehicles
        if (activeVehicles != null) {
            for (Vehicle v : activeVehicles) {
                int vx = (int) (v.drawX * SCALE) + OFFSET_X;
                int vy = (int) (v.drawY * SCALE) + OFFSET_Y;

                // Priority: Red (Waiting/Stuck) > Magenta (Smart Detour) > Blue (Normal)
                if (v.isWaitingOrSlow) {
                    g2d.setColor(Color.RED);
                } else if (v.hasRerouted) {
                    g2d.setColor(Color.MAGENTA);
                } else {
                    g2d.setColor(Color.BLUE);
                }

                g2d.fillOval(vx - 5, vy - 5, 10, 10);
                
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawOval(vx - 5, vy - 5, 10, 10);
            }
        }
    }
}

