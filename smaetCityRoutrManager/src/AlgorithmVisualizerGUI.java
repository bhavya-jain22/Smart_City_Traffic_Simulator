import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AlgorithmVisualizerGUI extends JFrame {

    private Map<Integer, Intersection> cityGraph;
    private Intersection startNode, targetNode;

    // A* state
    private PriorityQueue<NodeRecord> frontier;
    private Map<Intersection, Double> gCosts;
    private Map<Intersection, Road>   cameFrom;
    private Set<Intersection>          visited;
    private Intersection               currentProcessing;
    private boolean isFinished = false;
    private List<Road> finalPath = new ArrayList<>();

    // UI
    private JPanel      mapPanel;
    private JTextArea   explanationArea;
    private DefaultListModel<String> pqListModel;
    private JButton     stepBtn, playBtn, resetBtn;
    private JLabel      infoLabel;
    private JComboBox<String> algoCombo;
    private javax.swing.Timer autoTimer;

    // Auto-scale (computed in paintComponent)
    private int scale   = 16;
    private int offX    = 60;
    private int offY    = 60;

    private static class NodeRecord {
        Intersection node; double fCost;
        NodeRecord(Intersection n, double f) { node = n; fCost = f; }
    }

    // ── constructor ────────────────────────────────────────────────────
    public AlgorithmVisualizerGUI(Map<Integer, Intersection> graph) {
        super("🔍 Step-by-Step Algorithm Visualizer");
        this.cityGraph = graph;
        setSize(1200, 800);
        setLayout(new BorderLayout(4, 4));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(240, 242, 250));

        pickRandomNodes();
        buildTopBar();
        buildMapPanel();
        buildSidebar();
        buildLegend();

        initAlgorithm();
        setLocationRelativeTo(null);
    }

    // ── random start/target ────────────────────────────────────────────
    private void pickRandomNodes() {
        List<Intersection> nodes = new ArrayList<>(cityGraph.values());
        Random rand = new Random();
        startNode  = nodes.get(rand.nextInt(nodes.size()));
        targetNode = nodes.get(rand.nextInt(nodes.size()));
        while (startNode == targetNode) targetNode = nodes.get(rand.nextInt(nodes.size()));
    }

    // ── top control bar ───────────────────────────────────────────────
    private void buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        bar.setBackground(new Color(30, 34, 48));
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel title = new JLabel("Step-by-Step Pathfinding Visualizer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(241, 196, 15));

        // Algorithm selector
        JLabel algoLbl = new JLabel("Algorithm:");
        algoLbl.setForeground(Color.WHITE);
        algoLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        algoCombo = new JComboBox<>(new String[]{"A* (A-Star)", "Dijkstra", "Greedy (Best-First)"});
        algoCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        infoLabel = new JLabel("Start: " + startNode.getName() + "  →  Target: " + targetNode.getName());
        infoLabel.setForeground(new Color(180, 220, 255));
        infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        stepBtn  = fBtn("⏭ Next Step",  new Color(52, 152, 219));
        playBtn  = fBtn("▶ Auto Play",   new Color(46, 204, 113));
        resetBtn = fBtn("↺ Reset",       new Color(200, 90, 30));

        bar.add(title);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(algoLbl); bar.add(algoCombo);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(infoLabel);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(stepBtn); bar.add(playBtn); bar.add(resetBtn);

        add(bar, BorderLayout.NORTH);

        stepBtn.addActionListener(e -> step());

        playBtn.addActionListener(e -> {
            if (autoTimer != null && autoTimer.isRunning()) {
                autoTimer.stop();
                playBtn.setText("▶ Auto Play");
            } else {
                autoTimer = new javax.swing.Timer(350, ev -> {
                    if (isFinished) { autoTimer.stop(); playBtn.setText("▶ Auto Play"); }
                    else step();
                });
                autoTimer.start();
                playBtn.setText("⏸ Pause");
            }
        });

        resetBtn.addActionListener(e -> resetVisualizer());

        algoCombo.addActionListener(e -> resetVisualizer());
    }

    private JButton fBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setOpaque(true); b.setContentAreaFilled(true);
        b.setBackground(bg); b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 2),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    // ── map panel ──────────────────────────────────────────────────────
    private void buildMapPanel() {
        mapPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                computeScale();
                drawGraph((Graphics2D) g);
            }
        };
        mapPanel.setBackground(new Color(248, 250, 255));
        add(mapPanel, BorderLayout.CENTER);
    }

    // ── sidebar (PQ + explanation) ─────────────────────────────────────
    private void buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 6));
        sidebar.setPreferredSize(new Dimension(310, 0));
        sidebar.setBackground(new Color(240, 242, 250));
        sidebar.setBorder(new EmptyBorder(6, 4, 6, 6));

        // Priority Queue list
        pqListModel = new DefaultListModel<>();
        JList<String> pqList = new JList<>(pqListModel);
        pqList.setFont(new Font("Consolas", Font.PLAIN, 13));
        pqList.setBackground(new Color(248, 250, 255));
        JScrollPane pqScroll = new JScrollPane(pqList);
        pqScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52,152,219), 2),
            "Priority Queue (Frontier)", 0, 0,
            new Font("Segoe UI", Font.BOLD, 13), new Color(52,152,219)));

        // Explanation text area
        explanationArea = new JTextArea(10, 22);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setLineWrap(true);
        explanationArea.setEditable(false);
        explanationArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        explanationArea.setBackground(new Color(248, 250, 255));
        JScrollPane expScroll = new JScrollPane(explanationArea);
        expScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(46,204,113), 2),
            "Algorithm Explanation", 0, 0,
            new Font("Segoe UI", Font.BOLD, 13), new Color(46,204,113)));
        expScroll.setPreferredSize(new Dimension(0, 200));

        sidebar.add(pqScroll,  BorderLayout.CENTER);
        sidebar.add(expScroll, BorderLayout.SOUTH);
        add(sidebar, BorderLayout.EAST);
    }

    // ── colour legend ──────────────────────────────────────────────────
    private void buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        legend.setBackground(new Color(30, 34, 48));
        legend.add(dot(Color.GREEN,               "Start node"));
        legend.add(dot(Color.RED,                 "Target node"));
        legend.add(dot(Color.YELLOW,              "Currently expanding"));
        legend.add(dot(new Color(155,89,182),     "Visited"));
        legend.add(dot(new Color(52,152,219),     "In frontier (queue)"));
        legend.add(dot(new Color(46,204,113),     "Final path"));
        legend.add(dot(Color.LIGHT_GRAY,          "Unvisited"));
        add(legend, BorderLayout.SOUTH);
    }

    private JLabel dot(Color c, String text) {
        JLabel l = new JLabel("● " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(c);
        return l;
    }

    // ── algorithm init ────────────────────────────────────────────────
    private void initAlgorithm() {
        frontier = new PriorityQueue<>(Comparator.comparingDouble(nr -> nr.fCost));
        gCosts   = new HashMap<>();
        cameFrom = new HashMap<>();
        visited  = new HashSet<>();
        finalPath.clear();
        currentProcessing = null;
        isFinished = false;

        frontier.add(new NodeRecord(startNode, 0.0));
        gCosts.put(startNode, 0.0);

        String algo = (String) algoCombo.getSelectedItem();
        explanationArea.setText(
            "Algorithm: " + algo + "\n\n" +
            "Start: " + startNode.getName() + "\nTarget: " + targetNode.getName() + "\n\n" +
            "Initialization complete.\nStart node added to the Priority Queue with cost f=0.\n\n" +
            "Press 'Next Step' or 'Auto Play' to begin.");
        updateUIState();
        mapPanel.repaint();
    }

    // ── single step ───────────────────────────────────────────────────
    private void step() {
        if (isFinished) return;

        if (frontier.isEmpty()) {
            isFinished = true;
            explanationArea.setText("❌ Frontier is EMPTY.\nNo path exists from " +
                startNode.getName() + " to " + targetNode.getName() + ".");
            mapPanel.repaint();
            return;
        }

        currentProcessing = frontier.poll().node;
        visited.add(currentProcessing);

        // Target reached?
        if (currentProcessing.equals(targetNode)) {
            isFinished = true;
            // Trace back
            Intersection curr = targetNode;
            while (cameFrom.containsKey(curr)) {
                Road r = cameFrom.get(curr);
                finalPath.add(r);
                curr = r.getStart();
            }
            explanationArea.setText(
                "✅ TARGET REACHED!\n\n" +
                "Node: " + targetNode.getName() + "\n\n" +
                "Final path traced back through 'cameFrom' pointers.\n" +
                "Path length: " + finalPath.size() + " roads.\n" +
                "Total g-cost: " + String.format("%.1f", gCosts.getOrDefault(targetNode, 0.0)));
            updateUIState();
            mapPanel.repaint();
            return;
        }

        // Expand neighbors
        String algo = (String) algoCombo.getSelectedItem();
        StringBuilder sb = new StringBuilder();
        sb.append("📍 Expanding: ").append(currentProcessing.getName()).append("\n\n");

        for (Road edge : currentProcessing.getConnectedRoads()) {
            if (edge.isUnderConstruction()) continue;

            double gNew = gCosts.get(currentProcessing) + edge.getLength();

            if (gNew < gCosts.getOrDefault(edge.getEnd(), Double.MAX_VALUE)) {
                gCosts.put(edge.getEnd(), gNew);
                cameFrom.put(edge.getEnd(), edge);

                double hCost = heuristic(edge.getEnd(), algo);
                double fCost = computeF(gNew, hCost, algo);

                frontier.add(new NodeRecord(edge.getEnd(), fCost));
                sb.append("  → ").append(edge.getEnd().getName())
                  .append(" | g=").append(String.format("%.1f", gNew))
                  .append(" h=").append(String.format("%.1f", hCost))
                  .append(" f=").append(String.format("%.1f", fCost)).append("\n");
            }
        }

        sb.append("\nFrontier size: ").append(frontier.size());
        sb.append("\nVisited: ").append(visited.size()).append(" nodes");
        explanationArea.setText(sb.toString());

        updateUIState();
        mapPanel.repaint();
    }

    /** Heuristic value for a node given selected algorithm */
    private double heuristic(Intersection n, String algo) {
        if (algo != null && algo.startsWith("Dijkstra")) return 0;  // Dijkstra: h=0
        // A* and Greedy: Euclidean distance
        double dx = targetNode.getX() - n.getX();
        double dy = targetNode.getY() - n.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    /** f-cost: Greedy ignores g; Dijkstra ignores h; A* uses both */
    private double computeF(double g, double h, String algo) {
        if (algo != null && algo.startsWith("Greedy")) return h;      // Greedy: f = h only
        if (algo != null && algo.startsWith("Dijkstra")) return g;    // Dijkstra: f = g only
        return g + h;                                                  // A*: f = g + h
    }

    // ── reset ─────────────────────────────────────────────────────────
    private void resetVisualizer() {
        if (autoTimer != null) { autoTimer.stop(); playBtn.setText("▶ Auto Play"); }
        pickRandomNodes();
        infoLabel.setText("Start: " + startNode.getName() + "  →  Target: " + targetNode.getName());
        initAlgorithm();
    }

    // ── priority queue display ─────────────────────────────────────────
    private void updateUIState() {
        pqListModel.clear();
        // Copy & drain sorted order
        List<NodeRecord> sorted = new ArrayList<>(frontier);
        sorted.sort(Comparator.comparingDouble(nr -> nr.fCost));
        for (int i = 0; i < sorted.size(); i++) {
            NodeRecord nr = sorted.get(i);
            pqListModel.addElement(
                (i == 0 ? "▶ " : "   ") + nr.node.getName() +
                "  (f=" + String.format("%.1f", nr.fCost) + ")");
        }
    }

    // ── auto-scale ────────────────────────────────────────────────────
    private void computeScale() {
        if (cityGraph == null || cityGraph.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Intersection n : cityGraph.values()) {
            minX = Math.min(minX, n.getX()); maxX = Math.max(maxX, n.getX());
            minY = Math.min(minY, n.getY()); maxY = Math.max(maxY, n.getY());
        }
        int pw = mapPanel.getWidth(), ph = mapPanel.getHeight();
        if (pw < 50 || ph < 50) return;
        int gw = maxX - minX, gh = maxY - minY;
        if (gw == 0 || gh == 0) return;
        int margin = 45;
        int sx = (pw - 2*margin) / gw;
        int sy = (ph - 2*margin) / gh;
        scale = Math.max(1, Math.min(sx, sy));
        offX  = margin - minX * scale;
        offY  = margin - minY * scale;
    }

    // ── graph drawing ─────────────────────────────────────────────────
    private void drawGraph(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Roads
        for (Intersection node : cityGraph.values()) {
            for (Road road : node.getConnectedRoads()) {
                int x1 = node.getX()          * scale + offX;
                int y1 = node.getY()          * scale + offY;
                int x2 = road.getEnd().getX() * scale + offX;
                int y2 = road.getEnd().getY() * scale + offY;

                if (finalPath.contains(road)) {
                    g2d.setColor(new Color(46, 204, 113));
                    g2d.setStroke(new BasicStroke(4.0f));
                } else {
                    g2d.setColor(new Color(210, 210, 220));
                    g2d.setStroke(new BasicStroke(1.5f));
                }
                g2d.drawLine(x1, y1, x2, y2);
            }
        }

        // 2. Nodes
        for (Intersection node : cityGraph.values()) {
            int cx = node.getX() * scale + offX;
            int cy = node.getY() * scale + offY;

            Color c;
            int   sz;
            if (node.equals(startNode))          { c = Color.GREEN;               sz = 14; }
            else if (node.equals(targetNode))     { c = Color.RED;                 sz = 14; }
            else if (node.equals(currentProcessing)) { c = Color.YELLOW;           sz = 13; }
            else if (visited.contains(node))      { c = new Color(155, 89, 182);   sz = 9;  }
            else {
                boolean inF = false;
                for (NodeRecord nr : frontier) if (nr.node.equals(node)) { inF = true; break; }
                if (inF)                          { c = new Color(52, 152, 219);   sz = 9;  }
                else                              { c = new Color(180, 180, 190);  sz = 7;  }
            }

            g2d.setColor(c);
            g2d.fillOval(cx - sz/2, cy - sz/2, sz, sz);
            g2d.setColor(c.darker());
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawOval(cx - sz/2, cy - sz/2, sz, sz);

            // Label special nodes
            if (node.equals(startNode) || node.equals(targetNode) || node.equals(currentProcessing)) {
                g2d.setColor(new Color(30, 30, 30));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String lbl = node.equals(startNode) ? "S" : node.equals(targetNode) ? "T" : "★";
                g2d.drawString(lbl, cx + 7, cy - 4);
            }
            // Landmark names (non-Node_ prefixed)
            if (!node.getName().startsWith("Node_")) {
                g2d.setColor(new Color(0, 80, 160));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2d.drawString(node.getName(), cx + 7, cy + 12);
            }
        }
    }
}
