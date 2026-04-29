import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SimulationPanel extends JPanel {
    private Map<Integer, Intersection> cityGraph;
    private List<Vehicle> activeVehicles;
    private Consumer<String> logger;
    private java.util.function.Consumer<Road> onRoadBlocked;

    // Auto-scale each paint
    private int scale   = 16;
    private int offsetX = 60;
    private int offsetY = 60;

    // Flash ticker for animated elements
    private int flashTick = 0;

    // Congestion alert state
    private double peakDensityThisFrame = 0;
    private String mostCongestedRoadName = "";

    private final Color BG_DARK           = new Color(248, 250, 255);
    private final Color ROAD_NORMAL       = new Color(190, 195, 210);
    private final Color NODE_COLOR        = new Color(90, 95, 115);
    private final Color TEXT_CYAN         = new Color(0, 100, 200);

    public SimulationPanel(Map<Integer, Intersection> cityGraph,
                           List<Vehicle> activeVehicles,
                           Consumer<String> logger) {
        this.cityGraph      = cityGraph;
        this.activeVehicles = activeVehicles;
        this.logger         = logger;
        setBackground(BG_DARK);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public void setOnRoadBlocked(java.util.function.Consumer<Road> cb) { this.onRoadBlocked = cb; }

    public void advanceTick() { flashTick++; }

    // ── auto-scale ────────────────────────────────────────────────────
    private void computeScale() {
        if (cityGraph == null || cityGraph.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Intersection n : cityGraph.values()) {
            minX = Math.min(minX, n.getX()); maxX = Math.max(maxX, n.getX());
            minY = Math.min(minY, n.getY()); maxY = Math.max(maxY, n.getY());
        }
        int pw = getWidth(), ph = getHeight();
        if (pw < 50 || ph < 50) return;
        int gw = maxX - minX, gh = maxY - minY;
        if (gw == 0 || gh == 0) return;
        int margin = 50;
        scale   = Math.max(1, Math.min((pw - 2*margin)/gw, (ph - 2*margin)/gh));
        offsetX = margin - minX * scale;
        offsetY = margin - minY * scale;
    }

    // ── road click ────────────────────────────────────────────────────
    private void handleMouseClick(int mouseX, int mouseY) {
        if (cityGraph == null) return;
        Road closest = null;
        double minDist = Double.MAX_VALUE;
        for (Intersection node : cityGraph.values()) {
            for (Road road : node.getConnectedRoads()) {
                int x1 = road.getStart().getX() * scale + offsetX;
                int y1 = road.getStart().getY() * scale + offsetY;
                int x2 = road.getEnd().getX()   * scale + offsetX;
                int y2 = road.getEnd().getY()   * scale + offsetY;
                double d = ptSegDist(mouseX, mouseY, x1, y1, x2, y2);
                if (d < minDist && d < 12) { minDist = d; closest = road; }
            }
        }
        if (closest == null) return;

        boolean nowBlocked = !closest.isUnderConstruction();
        closest.setUnderConstruction(nowBlocked);
        for (Road rev : closest.getEnd().getConnectedRoads())
            if (rev.getEnd() == closest.getStart()) rev.setUnderConstruction(nowBlocked);

        if (nowBlocked) {
            int rerouted = 0;
            if (activeVehicles != null) {
                for (Vehicle v : activeVehicles) {
                    if (!v.isFinished() && v.currentPath != null && !v.currentPath.isEmpty()) {
                        Road next = v.currentPath.get(0);
                        if (next == closest ||
                            (next.getStart() == closest.getEnd() && next.getEnd() == closest.getStart())) {
                            v.justRerouted = true;
                            v.rerouteFlashTimer = 80;
                            rerouted++;
                        }
                    }
                }
            }
            if (logger != null) {
                logger.accept("🚧 ROAD BLOCKED: " + closest.getStart().getName() + " → " + closest.getEnd().getName());
                if (rerouted > 0)
                    logger.accept("🔄 REROUTING: " + rerouted + " vehicle(s) forced onto new path — flashing CYAN");
                else
                    logger.accept("ℹ  No vehicles were on this road right now.");
            }
            if (onRoadBlocked != null) onRoadBlocked.accept(closest);
        } else {
            if (logger != null)
                logger.accept("✅ UNBLOCKED: " + closest.getStart().getName() + " → " + closest.getEnd().getName());
        }
        repaint();
    }

    private double ptSegDist(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
        if (l2 == 0) return Math.hypot(px-x1, py-y1);
        double t = Math.max(0, Math.min(1, ((px-x1)*(x2-x1)+(py-y1)*(y2-y1))/l2));
        return Math.hypot(px-x1-t*(x2-x1), py-y1-t*(y2-y1));
    }

    // ── painting ──────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        computeScale();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        peakDensityThisFrame = 0;
        mostCongestedRoadName = "";

        // ── 1. Roads ──────────────────────────────────────────────────
        if (cityGraph != null) {
            for (Intersection node : cityGraph.values()) {
                int sx = node.getX() * scale + offsetX;
                int sy = node.getY() * scale + offsetY;
                for (Road road : node.getConnectedRoads()) {
                    int ex = road.getEnd().getX() * scale + offsetX;
                    int ey = road.getEnd().getY() * scale + offsetY;
                    double dens = road.getTrafficDensity();

                    // Track peak congestion for alert banner
                    if (dens > peakDensityThisFrame) {
                        peakDensityThisFrame = dens;
                        mostCongestedRoadName = road.getStart().getName() + "→" + road.getEnd().getName();
                    }

                    float thick;
                    Color roadColor;
                    boolean dashed = false;

                    if (road.isUnderConstruction()) {
                        roadColor = new Color(255, 140, 0);
                        thick = 3.5f; dashed = true;
                    } else if (dens >= 1.0) {
                        // JAMMED — deep red, very thick
                        roadColor = new Color(180, 0, 0);
                        thick = 7f;
                    } else if (dens >= 0.7) {
                        // CONGESTED — bright red
                        roadColor = new Color(220, 53, 69);
                        thick = 5f;
                    } else if (dens >= 0.4) {
                        // MODERATE — yellow-orange
                        roadColor = new Color(230, 160, 0);
                        thick = 3.5f;
                    } else {
                        roadColor = ROAD_NORMAL;
                        thick = 2f;
                    }

                    g2d.setColor(roadColor);
                    if (dashed) {
                        g2d.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND, 1f, new float[]{8, 5}, 0));
                    } else {
                        g2d.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    }
                    g2d.drawLine(sx, sy, ex, ey);

                    // ── Congestion % label on the road mid-point ──────
                    if (dens >= 0.5 && !road.isUnderConstruction()) {
                        int mx = (sx + ex) / 2;
                        int my = (sy + ey) / 2;
                        String pct = Math.min(100, (int)(dens * 100)) + "%";
                        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        // White shadow for readability
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(pct, mx-8, my+4);
                        g2d.setColor(roadColor.darker());
                        g2d.drawString(pct, mx-9, my+3);
                    }
                }
            }

            // ── 2. Nodes + traffic lights ─────────────────────────────
            g2d.setStroke(new BasicStroke(1f));
            for (Intersection node : cityGraph.values()) {
                int cx = node.getX() * scale + offsetX;
                int cy = node.getY() * scale + offsetY;
                g2d.setColor(NODE_COLOR);
                g2d.fillOval(cx-3, cy-3, 6, 6);

                if (node.isHorizontalGreen) {
                    g2d.setColor(new Color(0, 220, 80));
                    g2d.fillRect(cx-7, cy-2, 3, 4); g2d.fillRect(cx+4, cy-2, 3, 4);
                    g2d.setColor(new Color(220, 40, 40));
                    g2d.fillRect(cx-2, cy-7, 4, 3); g2d.fillRect(cx-2, cy+4, 4, 3);
                } else {
                    g2d.setColor(new Color(220, 40, 40));
                    g2d.fillRect(cx-7, cy-2, 3, 4); g2d.fillRect(cx+4, cy-2, 3, 4);
                    g2d.setColor(new Color(0, 220, 80));
                    g2d.fillRect(cx-2, cy-7, 4, 3); g2d.fillRect(cx-2, cy+4, 4, 3);
                }

                if (!node.getName().startsWith("Node_")) {
                    g2d.setColor(TEXT_CYAN);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    g2d.drawString(node.getName(), cx+6, cy-5);
                }
            }
        }

        // ── 3. Vehicles ───────────────────────────────────────────────
        if (activeVehicles != null) {
            for (Vehicle v : activeVehicles) {
                if (v.isFinished()) continue;
                int vx = (int)(v.drawX * scale) + offsetX;
                int vy = (int)(v.drawY * scale) + offsetY;

                Color dot; int r = 5;
                if (v instanceof DeliveryTruck) {
                    dot = new Color(230, 126, 34); r = 7;
                } else if (v.isAmbulance) {
                    dot = (flashTick/5)%2 == 0 ? new Color(255,0,0) : new Color(60,60,255); r = 6;
                } else if (v.justRerouted && v.rerouteFlashTimer > 0) {
                    v.rerouteFlashTimer--;
                    dot = (flashTick/4)%2 == 0 ? new Color(0,240,220) : new Color(255,240,0); r = 7;
                    if (v.rerouteFlashTimer == 0) v.justRerouted = false;
                } else if (v.isBottleneckCar) {
                    dot = new Color(180, 0, 140);  // deep magenta
                } else if (v.isWaitingOrSlow) {
                    dot = new Color(240, 60, 60);  // stuck = red
                } else if (v.hasRerouted) {
                    dot = new Color(130, 70, 180); // rerouted = purple
                } else {
                    dot = new Color(0, 140, 255);  // normal = blue
                }

                // Dot with border
                g2d.setStroke(new BasicStroke(1f));
                g2d.setColor(dot.darker());
                g2d.fillOval(vx-r-1, vy-r-1, (r+1)*2, (r+1)*2);
                g2d.setColor(dot);
                g2d.fillOval(vx-r, vy-r, r*2, r*2);
            }
        }

        // ── 4. CONGESTION ALERT BANNER (top-right) ────────────────────
        // Only shown when density ≥ 0.7 on at least one road
        if (peakDensityThisFrame >= 0.7) {
            drawCongestionBanner(g2d);
        }
    }

    private void drawCongestionBanner(Graphics2D g2d) {
        boolean jam = peakDensityThisFrame >= 1.0;
        Color bannerBg = jam ? new Color(160, 0, 0, 220) : new Color(200, 60, 0, 200);
        String line1   = jam ? "🔴 TRAFFIC JAM DETECTED!" : "🟡 CONGESTION ALERT!";
        String line2   = String.format("Peak density: %.0f%%  |  Road: %s",
                          Math.min(100, peakDensityThisFrame * 100), mostCongestedRoadName);

        // Pulse: alternate opacity
        boolean pulse = (flashTick / 6) % 2 == 0;
        if (pulse) {
            bannerBg = new Color(bannerBg.getRed(), bannerBg.getGreen(),
                                 bannerBg.getBlue(), jam ? 240 : 210);
        }

        int bw = getWidth() - 60, bh = 54;
        int bx = 30, by = 10;

        g2d.setColor(bannerBg);
        g2d.fillRoundRect(bx, by, bw, bh, 12, 12);
        g2d.setColor(jam ? new Color(255, 100, 100) : new Color(255, 200, 0));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(bx, by, bw, bh, 12, 12);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(line1, bx + 14, by + 22);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.setColor(new Color(255, 220, 180));
        g2d.drawString(line2, bx + 14, by + 42);
    }
}
